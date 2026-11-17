# pn-notification-cost-service


---

### Test locale del contratto API interno

#### Integrazione HTTP con MiniStack

Prerequisiti: JDK 21, Maven configurato per le dipendenze PagoPA e un runtime
container attivo con API compatibile Docker (Docker oppure Podman configurato
per Testcontainers). Non serve un emulatore gia avviato sulla porta 4566.

```sh
./mvnw -Papi-contract verify
```

Il profilo esegue i normali test unitari e poi soltanto
[MinistackApiContractIT](src/test/java/it/pagopa/pn/notificationcostservice/rest/MinistackApiContractIT.java):
microservizio completo, porta HTTP casuale, SDK AWS e listener reali, nessun mock
Java. Un container MiniStack 1.5.41 dedicato, fissato al digest multiarch, ospita
DynamoDB, SQS ed EventBridge. Lo smoke iniziale verifica anche condizioni
annidate DynamoDB, indice `iun_gsi` e routing EventBridge verso SQS; un problema
di compatibilita dell'emulatore impedisce l'avvio dei casi HTTP.

Credenziali sintetiche, endpoint locali, risorse e identificativi isolati sono
impostati dalla suite. Non usare credenziali AWS personali per l'applicazione
di test. L'eventuale `CODEARTIFACT_AUTH_TOKEN` serve solo a Maven e deve essere
disponibile nella stessa shell del comando.

La suite copre tutte le operazioni e gli status dichiarati nella
[specifica interna](docs/openapi/api-private.yaml), i vincoli annidati e i limiti
di cardinalita/numerici, coercizioni di tipo, path, body malformati e negoziazione
dei media type. Verifica inoltre rappresentazioni analogiche, somme descritte
nel contratto e sensibilita del validatore a risposte mutate. I POST sono
sincronizzati tramite outcome correlati, senza sottrarre messaggi al consumer;
il `202` resta distinto dal completamento della persistenza.

La [preparazione della specifica](src/test/java/it/pagopa/pn/notificationcostservice/rest/InternalApiSpecification.java)
normalizza in memoria le catene di alias `$ref` prima di Swagger Parser,
evitando autoriferimenti introdotti da alias esterni omonimi. Non modifica
documenti originali, URL/commit remoti o vincoli. Cicli di soli alias e target
mancanti interrompono esplicitamente la preparazione; gli schemi oggetto
ricorsivi e i valori di esempio sono preservati.
I [test di regressione](src/test/java/it/pagopa/pn/notificationcostservice/rest/InternalApiContractCasesTest.java)
verificano che un `Problem` valido passi e che required, tipi e cardinalita
errati siano rifiutati per i vincoli effettivi, non per errori di risoluzione.

Un proxy HTTP locale, senza dipendenze aggiuntive, provoca fault selettive su
`GetItem`, `SendMessage` e `UpdateItem`: il PUT deve fallire durante
l'aggiornamento, non nel controllo di esistenza. Ogni fault verifica il
successivo ripristino sulla stessa operazione. Nessun reset globale, sleep
fisso, accesso ad AWS reale o import dei test LocalStack esistenti.

Report prodotti anche quando il gate rileva difformita:

- [Failsafe](target/failsafe-reports/): esito dei singoli casi e motivazioni;
- [matrice](target/api-contract/coverage.json): coppie operazione/status
  esercitate, osservate e conformi allo schema;
- [osservazioni HTTP](target/api-contract/observations.json): status atteso/reale
  e diagnostica OpenAPI per scenario;
- [log MiniStack](target/api-contract/ministack.log).

**Il gate e intenzionalmente rosso se il servizio viola il contratto.**
Il raggiungimento di uno status non ne dimostra la conformita di body/header,
ne rende corrette le somme economiche. I report Failsafe includono anche queste
asserzioni. Non vengono corretti automaticamente OpenAPI o codice di produzione.
MiniStack non certifica l'equivalenza con AWS; restano esclusi carico,
idempotenza/concorrenza non specificate e protezioni di gateway.

`./mvnw test` resta indipendente da Docker. Gli altri IT LocalStack non sono
selezionati dal profilo `api-contract`. Senza questo profilo, anche `./mvnw verify`
esclude la suite MiniStack; gli altri IT mantengono la selezione standard e
i propri prerequisiti. Le classi di test vengono comunque compilate e non sono
incluse nel JAR applicativo. Piano e checklist sono in
[tasks/plan.md](tasks/plan.md) e [tasks/todo.md](tasks/todo.md).
Il [report leggibile del contratto](docs/report-contratto-api-interno.md)
riassume le difformita osservate e i task proposti per risolverle.
Il [piano dei test TDD](docs/piano-test-contratto-api-tdd.md) descrive scenari,
oracoli e criteri Red-Green-Refactor per verificarne il rientro.

### Consumer

Il consumer ascolta la coda SQS `pn-notification-cost-to-update` e gestisce gli eventi `NOTIFICATION_COST_INITIALIZATION`.

#### Consumer `InternalQueueConsumer`
Per ogni messaggio ricevuto:

- avvia il processo `UPDATE NOTIFICATION COST`
- imposta il contesto di logging (MDC) con informazioni utili come `messageId`, `traceId` e `iun`
- delega la gestione del messaggio al router interno
- al termine dell’elaborazione pubblica un evento di outcome su EventBridge con stato `OK`
- in caso di errore, traccia l’eccezione nei log e rilancia l’errore

La gestione è asincrona: l’API pubblica un messaggio in coda e il consumer esegue la persistenza reale dei dati.

#### Router
Il router ha la responsabilità di instradare il messaggio verso l’handler corretto in base al tipo evento.

Nel dettaglio:

- estrae il tipo evento dal messaggio tramite la configurazione di routing
- se il tipo evento è `NOTIFICATION_COST_INITIALIZATION`, inoltra il payload al relativo handler
- recupera `eventId` e `iun` per arricchire il contesto MDC prima della lavorazione
- se il tipo evento non è presente o non è supportato, genera un errore di routing

Attualmente il router supporta il solo evento:

- `NOTIFICATION_COST_INITIALIZATION`

#### NotificationCostInitializationEventHandler
L’handler che gestisce `NOTIFICATION_COST_INITIALIZATION`:

- estrae `iun`, `notificationCosts` e `payments`
- salva i record nella tabella `NotificationDeliveryCost`
- aggiorna i record nella tabella `PaymentInfo`
- gestisce eventuali errori di persistenza con log dedicati

#### Evento finale pubblicato
Al completamento della gestione, il servizio pubblica un evento su EventBridge contenente:

- `iun`
- `status = OK`

# Esecuzione dello script
Per eseguire correttamente lo script, è necessario configurare l'ambiente come segue:

### Ambiente locale:
È necessario prima avviare LocalStack e successivamente eseguire lo script di inizializzazione del consumer:
1. Avviare LocalStack con il comando (in un terminale separato):  
   `sudo docker compose up localstack`
2. Una volta che LocalStack è in esecuzione ed è raggiungibile su `http://localhost:4566`, eseguire da host lo script `init-for-consumer.sh` per inizializzare le risorse necessarie al consumer.

### pn-NotificationDeliveryCost

### Configurazione
- **Variabile d'ambiente**: `PN_NOTIFICATIONCOSTSERVICE_NOTIFICATIONDELIVERYCOSTTABLE_TABLENAME`
- **Nome risorsa CloudFormation**: `NotificationDeliveryCostTableName`
- **Tipo**: Tabella DynamoDB
- **Funzionalità**: La tabella pn-NotificationDeliveryCost viene utilizzata per persistere i costi di consegna delle notifiche.

---

### pn-PaymentInfo

### Configurazione
- **Variabile d'ambiente**: `PN_NOTIFICATIONCOSTSERVICE_PAYMENTINFOTABLE_TABLENAME`
- **Nome risorsa CloudFormation**: `PaymentInfoTableName`
- **Tipo**: Tabella DynamoDB
- **Funzionalità**: La tabella pn-PaymentInfo viene utilizzata per persistere le informazioni di pagamento associate a una notifica.

---

### pnCostToUpdate

### Configurazione
- **Variabile d'ambiente**: `PN_NOTIFICATIONCOSTSERVICE_TOPICS_PNNOTIFICATIONCOSTTOUPDATE`
- **Nome risorsa CloudFormation**: `PnNotificationCostToUpdateQueueName`
- **Tipo**: Input (Coda SQS)

### Funzionamento
- **Scopo**: Disaccoppia la richiesta di validazione dei dati di pagamento dalla logica di business. Il servizio invia un messaggio alla coda.
- **Trigger**: Riceve un evento di tipo `NotificationCostInitializationEvent`.
---

### populateNotificationDeliveryCost

#### Responsabilità
- **Esegue una migrazione di dati per calcolare e salvare i costi di consegna delle notifiche.**
- In base al numero degli items della lista di oggetti `recipients` presenti nella notifica, crea n record corrispondenti recuperando per ciascuno il relativo recipientId e popolando il relativo campo `recipientInternalId.`
- - Analizza gli item della tabella `pn-Timelines` filtrando per le seguenti categorie:
      - **SEND_ANALOG_DOMICILE**: Vengono estratti i campi `sentAttemptMade`, `productType` e `analogCost`.
      - Se `sentAttemptMade` è uguale a `0`, vengono popolati i campi relativi al primo invio (`firstAnalogCost`).
      - Se `sentAttemptMade` è uguale a `1`, vengono popolati i campi relativi al secondo invio (`secondAnalogCost`).
- **SEND_SIMPLE_REGISTERED_LETTER**: Vengono recuperati i campi `productType` e `analogCost` (come `cost`) per popolare `simpleRegisteredLetterCost`.
- **REQUEST_REFUSED** o **NOTIFICATION_CANCELLED**: Il flag `isDeleted` viene impostato a `true` per tutti i record creati sulla tabella `NotificationDeliveryCost`.
- **Trasforma i dati raccolti in un nuovo item.**
- **Salva l'item in una nuova tabella DynamoDB (`NotificationDeliveryCost`).**

#### Funzionalità
Questo script esegue un processo di migrazione per popolare la tabella `NotificationDeliveryCost`. 
Analizza le notifiche e le relative timeline per calcolare i costi di consegna.

#### Configurazione
| Variabile Ambiente | Descrizione                                | Default | Obbligatorio |
|--------------------|--------------------------------------------|---------|--------------|
| AWS_REGION         | La regione AWS per le operazioni DynamoDB. | -       | No           |
| AWS_PROFILE        | profilo AWS per le operazioni DynamoDB     | -       | No           |
| MFA_TOKEN          | token AWS per le operazioni DynamoDB       | -       | No           |
| NODE_ENV           | Env per l'esecuzione                       | `local` | Si           |

*Nota: Obbligatori solo per ambienti diversi da local.

# Esecuzione dello script
Prima di eseguire lo script, è necessario assicurarsi che le tabelle DynamoDB `pn-Timelines`, `NotificationDeliveryCost` e `PaymentInfo` siano correttamente configurate e accessibili.
Installare le dipendenze del progetto con `npm install`.
Per eseguire correttamente lo script, è necessario configurare l'ambiente come segue:

# Ambienti non locali (es. dev):
Creare un file `.env` nel percorso `src/config/.env` con la seguente struttura:
Snippet di codice
`NODE_ENV=dev`

# Configurazione AWS
`AWS_PROFILE=dev
MFA_TOKEN=123456
AWS_REGION=eu-south-1`
Assicurarsi di associare l'`AWS_PROFILE` corretto in base all'ambiente di destinazione e di inserire un `MFA_TOKEN` valido 
al momento del lancio.

# Ambiente locale:
In locale è sufficiente inizializzare la variabile `NODE_ENV` a `local`. È necessario avviare LocalStack eseguendo 
il file `init-for-migration.sh`, quindi lanciare il comando:

Bash
`NODE_TLS_REJECT_UNAUTHORIZED=0 node index.js IUN-STANDARD-MIX IUN-SIMPLE-LETTER`

