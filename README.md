# pn-notification-cost-service


---

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

