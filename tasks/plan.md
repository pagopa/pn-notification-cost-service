# Piano: test di integrazione del contratto API interno con MiniStack

## Obiettivo

Avviare il microservizio completo con AWS simulato da MiniStack e verificarne
le sole API HTTP interne contro `docs/openapi/api-private.yaml`.
Routing, binding, validazione, serializzazione, service, mapper e DAO devono essere
quelli reali. Non utilizzare `@WebFluxTest`, `@MockitoBean` o chiamate dirette al
controller come prova di conformita HTTP.

Implementato il 16 novembre 2026: il profilo `api-contract` avvia la suite
MiniStack descritta sotto, senza correggere le difformita del contratto.
Validatore e fixture sono condivisi con le regressioni di preparazione della
specifica. La suite WebFlux isolata e stata rimossa su richiesta; la verifica
HTTP del contratto resta affidata alla suite MiniStack.

Due esecuzioni consecutive del comando completo hanno prodotto lo stesso esito:
210 casi di integrazione, 188 fallimenti di conformita, zero errori e zero skip.
Tutte le 18 coppie operazione/status sono state esercitate e osservate; solo
cinque hanno anche body/header conformi allo schema. I 229 test ordinari passano,
anche con endpoint Docker non disponibile. Il gate resta rosso intenzionalmente.

Aggiornamento dopo la normalizzazione degli alias: 239 test ordinari passati
(incluse dieci regressioni su preparazione e `Problem`); nuovo run di 210 IT,
188 fallimenti, zero errori/skip. Non compare piu `loops on itself`.
La verifica effettiva del body rileva ora 59 osservazioni con `errors[0].element`
null, non ammesso dal tipo stringa dichiarato. Il conteggio dei test falliti
resta uguale perche questi scenari fallivano gia per il media type.

## Architettura realizzata

```text
JUnit / Failsafe
    |
    | HTTP reale su localhost e porta casuale
    v
Microservizio completo
@SpringBootTest(webEnvironment = RANDOM_PORT)
    |
    | SDK AWS reali
    v
MiniStack in un GenericContainer Testcontainers dedicato
    +-- DynamoDB: costi, pagamenti, indice iun_gsi
    +-- SQS: inizializzazione, verifica pagamenti
    +-- EventBridge: bus di outcome
    +-- SQS: cattura degli outcome per sincronizzare le fixture
```

- Usare Testcontainers gia presente nel POM, con `GenericContainer`, non
  `LocalStackContainer`. MiniStack e il progetto
  [ministackorg/ministack](https://github.com/ministackorg/ministack), licenza MIT.
  La release verificata nella ricerca del 16 novembre 2026 e
  [v1.5.41](https://github.com/ministackorg/ministack/releases/tag/v1.5.41).
  Immagine fissata al digest multiarch
  `sha256:aef90fec1a4581c5fa7b4f79d347898f8475111bac95bbab3736f4a44f5f8573`,
  con manifest linux/amd64 e linux/arm64; esecuzione verificata su ARM64.
- L'applicazione gira realmente con il suo intero contesto e listener, ed e
  chiamata attraverso una connessione HTTP. Non si pretende, in questa prima
  soluzione, di testare il JAR in un processo separato: questo richiederebbe
  ulteriore orchestrazione senza migliorare automaticamente i casi di contratto.
- Nessun Docker Compose, Dockerfile o framework Python aggiuntivo e necessario
  per la prima implementazione. Docker deve essere disponibile, ma non va
  montato il socket Docker dentro MiniStack per i tre servizi necessari.
- Riutilizzare il validatore OpenAPI gia aggiunto al POM, previa conferma della
  scelta nella prima attivita. La specifica originale rimane la fonte canonica;
  niente schemi riscritti o risposta JSON confrontata solo con DTO generati.
- L'accesso SDK AWS serve solo al bootstrap, alla preparazione/verifica delle
  fixture e alle fault controllate. La superficie sotto test sono le cinque
  operazioni HTTP interne, non le API di MiniStack o la suite completa dei consumer.
- Gli effetti asincroni sono usati per preparare e sincronizzare i test dei POST:
  il 202 conferma accettazione, non salvataggio completato.

## Evidenze dal repository

- [Configurazione locale](../config/application.properties): sono presenti due
  famiglie di client da indirizzare a MiniStack:
  `aws.endpoint-url` e `spring.cloud.aws.endpoint`. Sono configurati costi,
  pagamenti, due code e bus EventBridge.
- [Configurazione Testcontainers esistente](../src/test/java/it/pagopa/pn/notificationcostservice/LocalStackTestConfig.java):
  riusa LocalStack e modifica proprieta di sistema globali; non va importata
  nella nuova suite.
- [Bootstrap esistente](../src/test/resources/testcontainers/init.sh):
  definisce tabelle, indice e inizializzazione SQS.
  [Bootstrap consumer](../src/test/resources/testcontainers/init-for-consumer.sh):
  mostra bus, regola e coda di cattura dell'outcome. Riutilizzare gli schemi
  delle risorse, non i profili AWS personali o gli endpoint fissi.
- I listener attivi comprendono
  [inizializzazione](../src/main/java/it/pagopa/pn/notificationcostservice/middleware/queue/consumer/InternalQueueConsumer.java)
  e [verifica pagamenti](../src/main/java/it/pagopa/pn/notificationcostservice/middleware/queue/consumer/PaymentsToVerifyConsumer.java):
  anche la seconda coda deve esistere per avviare il contesto completo.

## Isolamento e sicurezza

- Container per esecuzione, porte allocate dinamicamente, credenziali AWS
  sintetiche e regione coerente per applicazione e fixture.
- Applicare endpoint e credenziali esplicite a entrambe le famiglie di SDK.
  Non usare SSO, profili personali o credenziali di produzione nell'applicazione
  di test. L'autenticazione CodeArtifact resta un prerequisito separato del build.
- Nessuna chiamata AWS pubblica durante i test applicativi. Verificare che tutti
  i client usino il container, non soltanto DynamoDB.
- Nessun reset globale di MiniStack condiviso. Risorse e dati appartengono al
  container della suite. Test serializzati, identificativi distinti per scenario,
  cleanup delle sole risorse e processi avviati dalla suite.
- Non usare `Thread.sleep` come sincronizzazione: polling con timeout e
  diagnostica esplicita. Non consumare la coda di inizializzazione dal test,
  perche si sottrarrebbero messaggi al consumer reale.

## Matrice di verifica

| API | Scenari HTTP dichiarati | Preparazione / osservazione |
|---|---|---|
| GET costo destinatario | 200, 400, 404, 500 | Record coerenti preinseriti; record assente/eliminato; input invalidi; fault DynamoDB isolata. |
| GET costo pagamento | 200, 400, 404, 500 | Pagamento e costo coerenti; pagamento/costo assente; input invalidi; fault DynamoDB. |
| POST inizializzazione legale | 202, 400, 500 | Pubblicazione reale; attesa dell'outcome e lettura HTTP del costo; input invalidi senza effetti; fault SQS del producer. |
| POST inizializzazione informale | 202, 400, 500 | Accettazione reale; outcome e fixture pagamenti; non presumere la disponibilita di un costo legale via GET; fault SQS. |
| PUT invalidazione | 204, 400, 404, 500 | Costi analogici preinseriti; conferma via GET delle fasi invalidate; input invalidi/not-found; fault sul percorso di aggiornamento. |

Obiettivo misurabile: tutte le cinque operazioni e tutte le 18 coppie
operazione/status dichiarate, con status, header e body validati contro OpenAPI.
Questo non significa esaustivita su tutti gli input possibili.

### Robustezza degli input

- Required mancanti e null, anche negli oggetti annidati; body assente e JSON
  malformato; tipi non coerenti, enum sconosciuti e pattern invalidi.
- `costRecipients`: 0, 1, 100, 101; pagamenti informali: 0, 1, 100, 101;
  `vat`: -1, 0, 100, 101; `paFee`: -1 e 0.
- IUN legale/informale, creditorTaxId, noticeCode e recIndex ai limiti dichiarati.
- Array vuoti, duplicati e campi opzionali: distinguere cio che OpenAPI ammette
  da nuove regole desiderate. Non introdurre rifiuti arbitrari.
- Content negotiation: osservare risposte con `Accept`/`Content-Type` non
  supportati. Se compaiono 406/415 non documentati, segnalarli come difformita,
  non aggiungerli al contratto solo per far passare i test.

### Robustezza delle risposte

- GET con zero, uno e due tentativi analogici; presenza/omissione/null dei
  dettagli; `partialCost`, enum e date.
- Verificare somme descritte nel contratto per `DELIVERY_MODE`, `FLAT_RATE` e
  `applyCost` vero/falso, riportando eventuali contraddizioni da decidere con
  l'owner. Non inventare nuove regole economiche.
- `Problem` e media type corretti per 400/404/500; 202 testuale e 204 senza body.
- Mutazioni locali di una risposta catturata: campo richiesto rimosso,
  enum/data invalidi, status non documentato. Il validatore deve rilevarle.
- Le fault del backend devono attraversare SDK e HTTP reali: non simulare
  gli errori sostituendo service o DAO con mock Java.

## Attivita e dipendenze

Le checklist dettagliate sono in [todo.md](todo.md).

```text
T1 Compatibilita MiniStack
  |
  v
T2 Avvio completo su HTTP reale
  |
  v
T3 Due GET + validatore OpenAPI
  |
  v
T4 Due POST asincroni ------> T5 PUT invalidazione
  |                              |
  +-------------+----------------+
                v
T6 Robustezza input/risposte
                |
                v
T7 Fault AWS e risposte 500
                |
                v
T8 Comando locale, report e chiusura
```

La sequenza e intenzionalmente semplice: definire una sola orchestrazione e una
sola strategia di fixture prima di estendere i casi. Non creare pool di agenti,
suite duplicate o configurazioni indipendenti per ogni endpoint.

## Esecuzione

Comando locale:

```sh
./mvnw -Papi-contract verify
```

Il profilo `api-contract` seleziona solo `MinistackApiContractIT` in Failsafe,
senza trascinare i test esistenti basati su LocalStack. Il normale `./mvnw test`
continua a funzionare senza Docker. Nessun test ordinario e disabilitato.

Report: `target/failsafe-reports/`, con nomi leggibili per operationId,
status e vincolo verificato; matrice `target/api-contract/coverage.json`,
osservazioni `target/api-contract/observations.json` e log MiniStack.
Una violazione reale del contratto deve produrre exit code diverso da zero.
Un'incompatibilita dell'emulatore deve essere distinta da un difetto dell'API.

## Rischi e mitigazioni

| Rischio | Mitigazione |
|---|---|
| MiniStack dichiara supporto ma differisce da AWS | T1 prova le operazioni precise usate dal servizio prima di implementare la suite. Nessuna certificazione di equivalenza ad AWS. |
| Test pendenti per code o outcome | Readiness esplicita, timeout finiti, diagnostica e identificativi per scenario. |
| Interferenza tra listener e fixture | Un container dedicato, scenari seriali e nessun reset durante la vita dei listener. |
| Fault SQS/DynamoDB non disponibili nativamente | Provare una fault localizzata con proxy solo se necessario e approvato; non promettere funzionalita MiniStack non verificate. |
| Errori del consumer confusi con esito HTTP | Separare 202 del producer e completamento asincrono. Una failure dopo il 202 non deve essere attesa come 500 della richiesta gia conclusa. |
| Build o riferimenti OpenAPI richiedono rete | Documentare CodeArtifact e risoluzione dei ref, separati dall'isolamento AWS a runtime. |
| Difformita gia presenti producono test rossi | Conservare i test e le evidenze; correggere contratto o implementazione con attivita approvate separate. |

## Fuori perimetro

- Test diretti di altre API o microservizi, migrazioni e Lambda esterne.
- Audit di tutte le API AWS simulate, test di carico e comportamento AWS reale.
- Garanzie nuove di idempotenza, atomicita o concorrenza non concordate.
- Modifica di API/status/schemi solo per rendere verde la suite.
- Migrazione dei test LocalStack esistenti: la nuova suite non deve importarli,
  ma convertirli tutti e un intervento separato.

## Decisioni e limiti residui

- Piano approvato; validatore e fixture conservati per MiniStack e regressioni
  di preparazione. Suite WebFlux isolata successivamente rimossa su richiesta.
- Compatibilita MiniStack verificata dagli SDK e dal contesto applicativo completo.
- Proxy HTTP locale per fault selettive approvato, senza nuove dipendenze.
  Un test dedicato ne verifica selettivita, mancato inoltro e recupero; una
  mutation della selezione dell'operazione e stata rilevata e ripristinata.
- Le difformita includono media type degli errori, campi non nullable emessi
  come null, validazioni mancanti/coercizioni, 406/415 non documentati e somme
  economiche contraddittorie. La suite non e una certificazione di equivalenza
  MiniStack/AWS o di esaustivita su ogni input.
- Interpretazione delle contraddizioni economiche e rientro delle difformita
  restano decisioni dell'owner API; non sono state modificate produzione o OpenAPI.

## Piano di rientro delle difformita

Questo estende il lavoro precedente senza riaprire T1-T8: la suite e realizzata,
ma i problemi rilevati restano da correggere. Evidenze e motivazioni sono nel
[report leggibile](../docs/report-contratto-api-interno.md); i nuovi task R1-R10
sono nella [checklist](todo.md#rientro-delle-difformita).

### Sequenza proposta

1. R1: correggere la rappresentazione degli errori; R8: ottenere dall'owner API
   la decisione sulle regole economiche. R8 puo procedere senza attendere R1.
2. R4 e R5: chiudere input non conformi e null negli array.
3. Checkpoint: errori conformi e POST invalidi senza pubblicazione.
4. R6 e R3: chiudere required del PUT e rappresentazione post-invalidazione.
5. Checkpoint: invalidazione valida e invalida senza regressioni.
6. R2 e R7: allineare omissione dei dettagli e negoziazione.
7. R9, soltanto dopo R8: applicare la decisione economica approvata.
8. R10: riesecuzione finale e aggiornamento delle evidenze.

Le dipendenze vincolanti sono R7 -> R1, R9 -> R8 e R10 -> R1-R7/R9.
Il resto e un ordine operativo proposto, non una dipendenza tecnica.
R4, R5 e R6 possono toccare la stessa configurazione di binding/generazione:
implementarli in sequenza o coordinare esplicitamente le modifiche.
R2, R3 e R9 condividono mapper/modelli e richiedono analoga attenzione.

### Decisioni e rischi

- Non modificare direttamente sorgenti sotto `target`: intervenire su OpenAPI,
  configurazione di generazione o codice applicativo, secondo la causa provata.
- La validazione piu rigorosa puo rifiutare coercizioni oggi usate dai chiamanti:
  verificare i consumer prima del rilascio, senza dichiarare valide quelle
  richieste rispetto al contratto corrente.
- Handler e DTO `Problem` dipendono da `pn-commons`: distinguere configurazione
  locale e difetto condiviso prima di proporre un aggiornamento di dipendenza.
- R7 richiede approvazione del comportamento pubblico 406/415; R8 richiede
  approvazione delle regole economiche. Non scegliere il risultato osservato
  come nuova specifica soltanto per ottenere un gate verde.
- Per ogni task aggiungere soltanto regressioni mancanti e rieseguire la suite;
  usare i casi esistenti come prova di rientro, mantenendo visibili i residui.

Il piano e proposto per revisione: la sua scrittura non autorizza modifiche
economiche o cambiamenti al contratto. Nessuna correzione R1-R10 e stata
implementata con la redazione del report.
