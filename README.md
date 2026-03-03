# pn-notification-cost-service


### Script per la migrazione

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
| AWS_REGION         | La regione AWS per le operazioni DynamoDB. | -       | Si           |
| NODE_ENV           | Env per l'esecuzione                       | `local` | No           |

N.B. Per lanciare lo script in locale, bisogna tirare su localstack tirando su il file `init-for-migration.sh` 
poi lanciare il seguente comando: `NODE_TLS_REJECT_UNAUTHORIZED=0 node index.js`
Nel caso in cui si voglia eseguire lo script in un ambiente diverso da `local`, 
è necessario creare/modificare la configurazione nel file nel seguente path `../config/.env`.
