# Report del contratto API interno e piano di rientro

**Data:** 16 novembre 2026. **Stato:** difformita aperte; correzioni non ancora implementate.

## Sintesi

Le cinque operazioni interne sono raggiungibili e tutti gli status documentati
sono stati osservati. Tuttavia, il servizio non rispetta completamente il
contratto per formato degli errori, validazione degli input e alcune risposte
di successo. Esistono inoltre discrepanze economiche che richiedono una decisione
dell'owner API prima di cambiare il calcolo o la descrizione del contratto.

La precedente anomalia del validatore sui riferimenti `Problem` e stata risolta
con una normalizzazione in memoria, limitata ai test. Le segnalazioni sul body
riportate qui derivano dalla riesecuzione successiva alla correzione.

| Indicatore | Esito |
|---|---|
| Test ordinari | 239 passati, incluse 10 regressioni sulla preparazione della specifica |
| Test di integrazione MiniStack | 210 eseguiti: 22 passati, 188 falliti |
| Errori di esecuzione / test saltati | 0 / 0 |
| Coppie operazione/status documentate | 18 esercitate e osservate su 18 |
| Coppie con risposta conforme nella matrice | 5 su 18 |
| Errori `Problem loops on itself` | 0 |

**188 casi falliti non significano 188 difetti distinti.** Un problema condiviso
di serializzazione fa fallire molti scenari; uno stesso scenario puo contenere
piu violazioni. I conteggi per categoria sotto non vanno sommati tra loro.

## Perimetro e fonti

Il [piano dei test in formato TDD](piano-test-contratto-api-tdd.md) definisce
oracoli, scenari Given/When/Then, tracciabilita e ciclo Red-Green-Refactor.

Il test avvia il microservizio completo su HTTP reale e usa MiniStack per
DynamoDB, SQS ed EventBridge, senza mock Java dei componenti applicativi.
La fonte del contratto e la [specifica interna](openapi/api-private.yaml),
con i suoi riferimenti remoti fissati al commit.

Evidenze della riesecuzione:

- [Osservazioni HTTP](../target/api-contract/observations.json): scenario,
  status atteso/reale e diagnostica del validatore.
- [Matrice operazione/status](../target/api-contract/coverage.json).
- [Risultati JUnit/Failsafe](../target/failsafe-reports/TEST-it.pagopa.pn.notificationcostservice.rest.MinistackApiContractIT.xml):
  include le asserzioni economiche e sugli effetti applicativi.
- [Suite di integrazione](../src/test/java/it/pagopa/pn/notificationcostservice/rest/MinistackApiContractIT.java).

I file in `target` sono rigenerati a ogni esecuzione e rimossi da `clean`;
questo documento conserva la sintesi del run sopra indicato.
Il report riguarda le prove HTTP e di robustezza, non una nuova validazione
statica Spectral/ModI. Non certifica equivalenza MiniStack/AWS, carico,
concorrenza o esaustivita su ogni input.

### Come leggere la matrice

`schemaConformant: true` significa che almeno uno scenario della matrice per
quella coppia ha ricevuto lo status atteso senza diagnostica di conformita.
Non significa che tutti gli altri scenari dell'operazione siano corretti,
ne verifica da solo somme economiche ed effetti asincroni.

Le cinque coppie conformi sono i due GET `200`, i due POST `202` e il PUT `204`.
Le altre tredici coppie, relative agli errori, non sono conformi.

## Problemi riscontrati

### F1 - Formato delle risposte di errore

- **132 osservazioni:** `Content-Type: application/json` anziche
  `application/problem+json`.
- **59 osservazioni:** `errors[0].element` presente con valore `null`,
  mentre lo schema richiede una stringa quando la proprieta e presente.
- Impatto: un client conforme non puo affidarsi a media type e schema degli
  errori, anche quando lo status HTTP e corretto.
- Rientro: **R1**. Individuare il comportamento degli handler condivisi
  `pn-commons` prima di intervenire; non e ancora dimostrato che serva
  una modifica alla libreria anziche alla sua configurazione locale.

### F2 - Dettaglio analogico assente serializzato come null

`analogCostDetail` e opzionale, ma non nullable. Nei casi senza componenti
analogiche la rappresentazione espone invece `null`: **21 osservazioni**
in `totalCost.details` e **una** in `partialCost.details`.

Omissione e `null` non sono equivalenti in OpenAPI 3.0. Rientro: **R2**;
preferire una rappresentazione conforme al contratto vigente, senza rendere
nullable tutti i campi per aggirare il problema.

### F3 - Invalidazione perde un attributo richiesto

Dopo l'invalidazione, alcuni componenti analogici espongono `productType: null`.
La proprieta e richiesta e non nullable: **sei osservazioni per il primo
componente e sei per il secondo**. Non sono necessariamente dodici richieste
distinte, perche una risposta puo violare entrambi i vincoli.

Il controller crea l'aggiornamento di invalidazione con `productType(null)`;
va verificato il comportamento dell'updater e della persistenza prima di
stabilire dove preservare l'attributo. Rientro: **R3**.
Non basta omettere il campo richiesto, ne inventare un valore di default.

### F4 - POST accettano body non conformi

**16 richieste** attese come `400` sono accettate con `202`:

| Operazione | Esempi e campi coinvolti | Casi |
|---|---|---:|
| Inizializzazione legale | Numeri al posto di stringhe in `senderPaId`, `senderTaxId`, `recipientInternalId`; stringhe numeriche o frazioni in `paFee`, `vat`, `recIndex`; `applyCost: "true"`; `payments: null` | 11 |
| Inizializzazione informale | Numeri al posto di stringhe in `senderPaId`, `senderTaxId`, `recipientInternalId`; `recIndex` come stringa numerica o frazione | 5 |

Rientro: **R4**. Il contratto descrive i tipi del JSON ricevuto, non soltanto
il DTO ottenuto dopo le coercizioni. Nei pagamenti legali l'array opzionale
puo essere omesso, ma non per questo puo essere esplicitamente `null`.
L'input invalido deve essere respinto prima della pubblicazione su SQS.

### F5 - Elementi null negli array provocano 500

**Cinque richieste** attese come `400` restituiscono `500`:

- `costRecipients[0]: null` nei due POST;
- `payments[0]: null` nei due POST;
- `costPhases[0]: null` nel PUT.

Rientro: **R5**. Validare gli elementi e la cascata sugli oggetti annidati
prima di mapper, producer e updater; non trasformare genericamente ogni
eccezione applicativa in `400`.

### F6 - PUT accetta un campo required mancante

Il PUT senza `costPhases` restituisce `204`, anziche `400`, nonostante il campo
sia required nello schema. Rientro: **R6**. Verificare anche la generazione del
DTO: un valore iniziale vuoto non deve mascherare l'assenza del campo.
Non introdurre un divieto di array vuoto se il contratto non lo prevede.

### F7 - Risposte 406 e 415 non documentate

La negoziazione produce `406` sulle cinque operazioni e `415` sulle tre con
body: **otto osservazioni** non descritte dalla specifica.

Non e automaticamente sbagliato che il framework rifiuti quei media type:
e incompleto l'allineamento tra comportamento esposto e contratto.
Rientro: **R7**, con decisione esplicita dell'owner API. Aggiungere codici
senza verificare anche header e body non chiude il problema.

### F8 - Totali economici non riconciliabili con le descrizioni

Le descrizioni OpenAPI parlano di somma dei dettagli; nelle fixture analogiche:

| Scenario | Totale atteso sommando i dettagli / ricevuto | Parziale atteso / ricevuto |
|---|---|---|
| GET destinatario, `FLAT_RATE` | 333 / 0 eurocent | Non applicabile |
| GET pagamento, `FLAT_RATE`, `applyCost=true` | 333 / 0 eurocent | 250 / 0 eurocent |
| GET pagamento, `FLAT_RATE`, `applyCost=false` | 333 / 0 eurocent | 250 / 0 eurocent |
| GET pagamento, `DELIVERY_MODE`, `applyCost=false` | 333 / 0 eurocent | 250 / 0 eurocent |

Il caso pagamento `DELIVERY_MODE` con `applyCost=true` supera queste asserzioni.
I valori 333 e 250 appartengono alle fixture: non sono tariffe da codificare.
Non e ancora deciso se siano errati il calcolo, la rappresentazione dei dettagli
o la descrizione delle eccezioni per politica/applicazione del costo.
Rientro: **R8** per la decisione, poi **R9** per l'implementazione approvata.

## Task di rientro proposti

P1 indica una difformita funzionale/contrattuale da affrontare per prima;
P2 un allineamento documentale o di rappresentazione da completare comunque.
Non sono valutazioni di gravita di un incidente di produzione.
I task sono **da eseguire**, non correzioni gia applicate.

| Task | Priorita | Risultato atteso | Dipendenze |
|---|---|---|---|
| [R1](../tasks/todo.md#r1) - Errori conformi | P1 | Media type corretto e `Problem` valido per 400/404/500 | Nessuna |
| [R2](../tasks/todo.md#r2) - Dettaglio analogico opzionale | P2 | GET digitali conformi, anche nel parziale | Nessuna |
| [R3](../tasks/todo.md#r3) - Preservare productType | P1 | GET post-invalidazione con attributo richiesto valido | Nessuna |
| [R4](../tasks/todo.md#r4) - Validare body dei POST | P1 | I 16 input non conformi ricevono 400 senza pubblicazione | Nessuna |
| [R5](../tasks/todo.md#r5) - Rifiutare elementi null | P1 | I cinque casi ricevono 400 senza effetti applicativi | Nessuna |
| [R6](../tasks/todo.md#r6) - Required del PUT | P1 | Campo assente rifiutato, array vuoto trattato secondo contratto | Nessuna |
| [R7](../tasks/todo.md#r7) - Negoziazione dei media type | P2 | Decisione API approvata e otto casi completamente conformi | R1 |
| [R8](../tasks/todo.md#r8) - Decidere le regole economiche | P1 | Tabella approvata di politica, applyCost, totali e dettagli | Nessuna |
| [R9](../tasks/todo.md#r9) - Allineare contratto e calcolo | P1 | Le regole approvate sono descritte e provate senza contraddizioni | R8 |
| [R10](../tasks/todo.md#r10) - Chiusura del gate | P1 | Suite ordinaria e integrazione verdi, matrice completa | R1-R7, R9 |

Accettazione, file candidati, taglia e verifiche sono nella
[checklist operativa](../tasks/todo.md#rientro-delle-difformita).
Sequenza, checkpoint e rischi sono nel [piano aggiornato](../tasks/plan.md#piano-di-rientro-delle-difformita).

## Riproduzione e criterio di chiusura

Con JDK 21, autenticazione Maven disponibile e runtime container compatibile:

```sh
./mvnw -Papi-contract verify
```

Il comando deve terminare con exit code zero dopo il rientro, non prima.
Durante i singoli task il gate puo restare rosso per categorie ancora aperte:
confrontare gli scenari interessati nei report, senza saltare gli altri test.

La chiusura richiede tutti i casi presenti passati, nessuno saltato, le 18
coppie documentate conformi e le nuove eventuali coppie coperte.
La sola matrice verde non basta: devono passare anche robustezza, effetti
applicativi e asserzioni economiche approvate.
Restano vietati indebolimento arbitrario degli schemi, rimozione delle
asserzioni e riclassificazione dei fallimenti come successi.
