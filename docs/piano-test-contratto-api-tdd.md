# Piano dei test del contratto API interno - TDD

**Data:** 16 novembre 2026. **TDD:** Test-Driven Development.
**Stato:** specifica delle prove realizzate e del ciclo di rientro ancora aperto.

## 1. Obiettivo e uso del documento

Provare attraverso HTTP reale che le cinque operazioni interne rispettino
la [specifica OpenAPI](openapi/api-private.yaml), per richieste, risposte ed
effetti applicativi osservabili. Per ogni difetto il percorso e:

1. **Red:** avere un caso riproducibile che fallisce per la ragione attesa.
2. **Green:** correggere il minimo necessario per soddisfare quel caso,
   preservando quelli gia corretti.
3. **Refactor:** migliorare la soluzione senza modificarne il comportamento,
   poi rieseguire le prove interessate.

Questo e anche un riordino TDD della suite gia costruita: **non dimostra che
ogni test esistente sia stato scritto prima del relativo codice di produzione**.
I test rossi esistenti sono il punto di partenza del rientro.
La regressione sui riferimenti `Problem` ha invece un Red verificato prima
della normalizzazione e un Green verificato dopo.

Il [report dei problemi](report-contratto-api-interno.md) contiene le evidenze
e le priorita; i [task R1-R10](../tasks/todo.md#rientro-delle-difformita)
definiscono gli interventi. Qui si definiscono le prove e i loro oracoli,
non nuove regole funzionali.

## 2. Livelli e ambiente di prova

| Livello | Implementazione | Cosa dimostra |
|---|---|---|
| Preparazione del contratto | [InternalApiContractCasesTest](../src/test/java/it/pagopa/pn/notificationcostservice/rest/InternalApiContractCasesTest.java) | Alias risolti e vincoli `Problem` realmente applicati |
| Affidabilita della fault injection | [MinistackFaultProxyTest](../src/test/java/it/pagopa/pn/notificationcostservice/rest/MinistackFaultProxyTest.java) | Guasto selettivo, nessun inoltro della chiamata bloccata e recupero |
| Integrazione HTTP | [MinistackApiContractIT](../src/test/java/it/pagopa/pn/notificationcostservice/rest/MinistackApiContractIT.java) | Contratto sul microservizio completo, con SDK e listener reali |

Il livello HTTP usa `@SpringBootTest(RANDOM_PORT)` e un container MiniStack
dedicato con DynamoDB, SQS ed EventBridge. Non sostituisce controller, service,
mapper, DAO o producer con mock Java.
La suite WebFlux isolata e stata rimossa; il contratto HTTP viene verificato
dalla suite MiniStack. Validatore, fixture e regressioni di preparazione
restano necessari a questa suite.

### Prerequisiti e isolamento

- JDK 21, Maven wrapper e accesso alle dipendenze PagoPA; eventuale token Maven
  disponibile nella stessa shell, senza copiarlo nei file del progetto.
- Runtime container compatibile Docker per l'integrazione; immagine MiniStack
  fissata a versione/digest dalla suite.
- Credenziali AWS sintetiche, endpoint locali per tutte le famiglie di client,
  porte dinamiche, dati e identificativi distinti per scenario.
- Bootstrap verificato dagli SDK: operazioni DynamoDB effettivamente usate,
  indice `iun_gsi`, SQS e routing EventBridge verso la coda di cattura.
- Polling con timeout sugli outcome correlati; niente attese fisse e nessun
  consumo dal test della coda di inizializzazione usata dal servizio.
- Cleanup delle sole risorse create dalla suite, senza reset di ambienti condivisi.

I test ordinari non richiedono Docker. La preparazione della specifica canonica
richiede comunque rete per i riferimenti OpenAPI remoti: non e una suite
completamente offline.

## 3. Oracoli: come si decide se un caso passa

1. **Richiesta:** validatore OpenAPI sul JSON originale, prima delle coercizioni
   applicative. Le fixture positive devono essere conformi; le mutazioni
   negative devono violare il vincolo che intendono esercitare.
2. **Risposta:** status atteso, media type, schema del body, required, tipi,
   enum e formati. Per gli errori, anche `Problem.status == HTTP status`.
3. **Effetti:** assenza di scritture per richieste respinte; outcome e dati
   coerenti dopo le richieste accettate; rilettura HTTP dopo l'invalidazione.
4. **Semantica economica:** somme descritte dalla specifica corrente, segnalando
   le contraddizioni. Le eccezioni non vengono inventate dal test:
   occorre la decisione R8 prima di aggiornare gli oracoli economici.

Un media type errato resta un fallimento anche se un secondo passaggio,
con lo stesso body e il media type dichiarato, consente di diagnosticare
separatamente i vincoli del body. Questo passaggio non corregge la risposta
ricevuta e non la rende conforme.

## 4. Matrice minima del contratto

| ID | Operazione | Status da esercitare |
|---|---|---|
| API-01 | `getNotificationCost` - GET costo destinatario | 200, 400, 404, 500 |
| API-02 | `getNotificationCostByPayment` - GET costo pagamento | 200, 400, 404, 500 |
| API-03 | `initializeNotificationCost` - POST legale | 202, 400, 500 |
| API-04 | `initializeInformalNotificationCost` - POST informale | 202, 400, 500 |
| API-05 | `invalidatePaperCost` - PUT invalidazione | 204, 400, 404, 500 |

Sono 18 coppie operazione/status. La lista delle operazioni e confrontata
con i mapping reali del controller; fixture mancanti o aggiuntive sono errori
espliciti. Un nuovo status richiede uno scenario dedicato, non un risultato
positivo assegnato automaticamente.

## 5. Catalogo delle prove Given / When / Then

Gli ID identificano famiglie di prove, non il numero dei casi JUnit.
Le varianti derivate dagli schemi producono piu casi per ciascuna famiglia.

### CT-01 - Preparazione e sensibilita dello schema

**Given:** specifica canonica con alias esterni omonimi; fixture locali con
catene relative, oggetti ricorsivi ed esempi contenenti un `$ref` letterale.
**When:** si prepara il contratto e si validano body `Problem`.
**Then:**
- Un `Problem` valido e accettato; errors vuoto/assente, codice mancante,
  status assente/stringa/600 sono rifiutati per i vincoli effettivi.
- Nessun errore `loops on itself`; cicli di soli alias e target mancanti
  interrompono esplicitamente la preparazione.
- Oggetti ricorsivi, esempi e sorgenti originali non vengono alterati.

**TDD:** Red autoriferimento riprodotto; Green con normalizzazione in memoria.
Dieci regressioni passate. Mantenere queste prove prima di modificare il loader.

### CT-02 - Superficie HTTP e matrice degli status

**Given:** applicazione avviata, risorse predisposte e fixture valide.
**When:** si confrontano mapping/OpenAPI e si esegue ogni coppia API/status.
**Then:** operazioni coincidenti; status, header e body conformi; `202`
con corpo testuale `OK`, `204` senza corpo. Un errore non e conforme solo
perche ha lo status atteso.

**Implementazione:** `openApiAndControllerExposeExactlyTheSameOperations`,
`everyDocumentedOperationAndStatus`.
**Red aperto:** risposte di errore, R1.

### CT-03 - Rappresentazioni dei due GET

**Given:** costi con zero, uno o due tentativi analogici; dati presenti,
assenti, eliminati o pagamento presente senza costo associato.
**When:** si chiamano i due GET.
**Then:** `200` e schema completo per dati presenti; `404` con `Problem`
conforme per dati non disponibili. Dettagli opzionali omessi se assenti,
non sostituiti con null non ammessi.

**Implementazione:** `getResponsesAndAnalogRepresentations`,
`deletedCostsReturnNotFound`, `paymentWhoseCostIsAbsentReturnsNotFound`.
**Red aperto:** dettaglio analogico null, R2; formato errori, R1.

### CT-04 - Required, tipi e vincoli annidati

**Given:** un body valido per ciascun POST e per il PUT.
**When:** si muta un vincolo alla volta: campo required assente, null,
tipo errato, coercizione stringa/numero/booleano, frazione al posto di intero,
enum o pattern non valido.
**Then:** gli input invalidi ricevono `400` conforme e non causano scritture;
quelli ammessi dal contratto mantengono lo status positivo e gli effetti previsti.

I casi includono elementi null in `costRecipients`, `payments` e `costPhases`.
Il campo opzionale omesso non va confuso con un valore null esplicito.

**Implementazione:** `requiredTypesEnumsPatternsAndBoundsIncludingNestedObjects`
e generatore in
[InternalApiContractCases](../src/test/java/it/pagopa/pn/notificationcostservice/rest/InternalApiContractCases.java).
**Red aperto:** 16 POST accettati, cinque null con 500 e PUT required con 204;
R4, R5, R6, con formato errori R1.

### CT-05 - Limiti e parametri di path

**Given:** fixture coerenti con gli schemi, senza introdurre vincoli nuovi.
**When:** si provano i valori al limite e immediatamente fuori:

| Vincolo | Valori rappresentativi |
|---|---|
| Destinatari; pagamenti informali | 0, 1, 100, 101 elementi |
| `vat` | -1, 0, 100, 101 |
| `paFee` | -1, 0 |
| Interi int32 | Estremi ammessi dallo schema, overflow e underflow |
| `recIndex` nel path GET | Estremi int32 e valori fuori intervallo |
| `recIndex` testuale nel PUT | `RECINDEX_99`, `RECINDEX_100`, pattern invalido |
| IUN, creditorTaxId, noticeCode | Pattern/tipo non validi secondo i parametri dichiarati |

**Then:** accettazione o rifiuto secondo lo schema, senza coercizioni indebite.
Un parametro valido senza record puo produrre `404`, non necessariamente `200`.
Array vuoti e duplicati sono ammessi quando il contratto non li vieta.

**Implementazione:** generatore dei body, `pathParametersRejectInvalidPatternsLengthsAndTypes`,
`recipientIndexInt32BoundariesAreAcceptedAndOverflowIsRejected`.
I valori sopra descrivono partizioni coperte, non ogni combinazione possibile.

### CT-06 - Body e negoziazione

**Given:** endpoint con body e operazioni con media type dichiarati.
**When:** body assente/malformato, `Accept` non supportato oppure
`Content-Type` non supportato.
**Then:** nessuna scrittura; body assente/malformato rifiutato con `400`.
I test attuali attendono `406` per Accept e `415` per Content-Type non supportati,
ma segnalano che tali risposte non sono documentate.

**Implementazione:** `missingMalformedBodiesAndUnsupportedMediaTypes`.
**Red aperto:** otto risposte 406/415 non documentate, R7.
L'owner approva il contratto prima di consolidare il Green; documentare solo
lo status senza validarne header/body non e sufficiente.

### CT-07 - Accettazione asincrona dei POST

**Given:** payload valido e risorse AWS isolate.
**When:** si invia il POST e poi si attende l'outcome con lo stesso identificativo.
**Then:** `202` conferma l'accettazione, non il completamento della persistenza.
Dopo l'outcome, il costo legale e leggibile via HTTP; per l'informale si
verificano i pagamenti persistiti e l'assenza di un costo legale.

**Implementazione:** `everyDocumentedOperationAndStatus` con
`verifySuccessfulEffect`.
Non attendere un ipotetico `500` della richiesta gia conclusa se il consumer
fallisce dopo il `202`.

### CT-08 - Invalidazione e rilettura

**Given:** due tentativi analogici valorizzati.
**When:** si invalida una fase, entrambe, oppure si ripete l'invalidazione
di una sola fase.
**Then:** `204` vuoto, costo delle fasi selezionate a zero, altre fasi
invariate e GET successivo conforme, incluso `productType` richiesto.

**Implementazione:** scenari PUT della matrice,
`invalidatingOneAttemptCanBeRepeatedWithoutChangingTheOther` e verifica effetti.
**Red aperto:** `productType` null, R3.
La ripetizione copre queste fixture, non prova idempotenza generale o concorrenza.

### CT-09 - Guasti backend e recupero

**Given:** scenario valido e proxy selettivo per operazione e identificativo.
**When:** si bloccano `GetItem` per GET, `SendMessage` per POST o `UpdateItem`
per PUT; poi si rimuove il guasto.
**Then:** il guasto raggiunge la chiamata SDK prevista, la risposta e `500`
conforme e la stessa operazione torna a riuscire dopo il ripristino.
Nel PUT non basta far fallire la lettura preliminare.

**Implementazione:** `exerciseBackendFault` e test dedicato del proxy.
**Red aperto:** rappresentazione dei 500, R1.
Un guasto nell'avvio di MiniStack non vale come prova di questo scenario.

### CT-10 - Sensibilita alle risposte corrotte

**Given:** risposta catturata dai GET.
**When:** il validatore riceve copie mutate con required rimossi, valori
non conformi, enum/date errati dove previsti, o status non documentato.
**Then:** rileva le violazioni; le varianti valide rimangono accettate.

**Implementazione:** `validatorDetectsCorruptResponsesAndUndocumentedStatus`.
Queste sono mutazioni delle risposte fornite al validatore, non una campagna
generale di mutation testing del codice applicativo.

### CT-11 - Coerenza economica

**Given:** fixture analogiche con politiche `FLAT_RATE`/`DELIVERY_MODE` e,
nel GET pagamento, `applyCost` vero/falso.
**When:** si leggono totale, parziale e componenti.
**Then:** valgono le somme descritte dalla specifica, oppure le eccezioni
esplicitamente approvate dall'owner in R8.

**Implementazione:** `totalsEqualTheDocumentedSumOfDetails`.
**Red aperto:** quattro scenari del report, con somma totale 333 contro 0
e, dove presente, parziale 250 contro 0 eurocent.
**Green bloccato sulla decisione R8, poi R9:** non cambiare importi o rimuovere
asserzioni solo per ottenere un risultato positivo.

## 6. Protocollo TDD per i task di rientro

| Passo | Azione | Evidenza richiesta |
|---|---|---|
| Red | Selezionare il caso esistente o aggiungere una regressione mancante | Nome scenario, input, atteso/reale e causa del fallimento |
| Diagnosi | Separare difetto applicativo, contratto ambiguo e problema del test | Ipotesi verificata; decisione owner per R7/R8 |
| Green | Correzione minima nella sorgente appropriata | Casi interessati passati, nessun effetto vietato |
| Refactor | Eventuale semplificazione a comportamento invariato | Stesse prove passate dopo la modifica |
| Regressione | Eseguire i controlli dipendenti e confrontare il residuo | Nessun nuovo fallimento; difetti aperti ancora visibili |

Per una famiglia di casi dinamici usare inizialmente l'esecuzione dell'intera
suite di integrazione e i nomi Failsafe per isolare l'evidenza: non si assume
che un singolo caso dinamico sia selezionabile come un metodo JUnit ordinario.
I test unitari mirati si aggiungono soltanto per nuova logica o per una
regressione non gia provata, senza duplicare tutte le combinazioni HTTP.

Non modificare manualmente i DTO generati. Non saltare casi, abbassare vincoli,
ignorare i riferimenti remoti o convertire errori di preparazione in successi.

## 7. Comandi e report

Preparazione della specifica, senza container:

```sh
./mvnw -Dtest=InternalApiContractCasesTest test
```

Test ordinari, senza container:

```sh
./mvnw test
```

Test ordinari seguiti dalla sola integrazione MiniStack selezionata dal profilo:

```sh
./mvnw -Papi-contract verify
```

Senza `-Papi-contract`, Failsafe esclude la suite MiniStack anche durante
`verify`, senza disabilitare gli altri IT. Le classi vengono comunque compilate
nella fase di compilazione dei test.

Output:
- [Failsafe XML](../target/failsafe-reports/TEST-it.pagopa.pn.notificationcostservice.rest.MinistackApiContractIT.xml):
  esito completo, incluse somme ed effetti.
- [Osservazioni](../target/api-contract/observations.json):
  status e violazioni HTTP per scenario, non tutti i dettagli delle asserzioni.
- [Matrice](../target/api-contract/coverage.json):
  almeno uno scenario documentato conforme per coppia; non sostituisce
  i risultati di robustezza.
- [Log MiniStack](../target/api-contract/ministack.log).

Un errore di credenziali Maven, riferimento remoto o bootstrap e un blocco
di preparazione, non una prova Red di un difetto dell'API.
I report generati sono volatili e vengono rimossi da `clean`.

## 8. Baseline e criteri di completamento

Baseline osservata dopo la normalizzazione:
**239 test ordinari passati; 210 IT, 22 passati e 188 falliti,
zero errori/skip; 18/18 status osservati, 5/18 coppie conformi nella matrice.**
Il numero dei casi puo crescere per regressioni o nuove risposte approvate;
non deve diminuire per nascondere difetti.

### Checkpoint

- **A - Oracoli affidabili:** CT-01, bootstrap e selettivita del proxy verificati.
- **B - Errori e input:** R1/R4/R5 chiusi, richieste invalide senza scritture.
- **C - PUT e rappresentazioni:** R2/R3/R6 chiusi, GET/PUT conformi.
- **D - Decisioni di contratto:** R7 e R8 approvati; R9 implementato e verificato.
- **E - Gate finale:** R10, ordinari e integrazione verdi senza errori o skip;
  matrice completa, robustezza, effetti e semantica approvata passati.

Non basta che passino i 18 scenari della matrice. Non si dichiara esaustivita:
restano fuori test di carico, AWS reale, protezioni del gateway,
concorrenza/atomicita non specificate e combinazioni arbitrarie degli input.
