                                  # Attivita: contratto API interno con MiniStack

Piano: [plan.md](plan.md). Implementazione e verifiche T1-T8 completate il
16 novembre 2026. Le spunte indicano verifiche implementate ed eseguite, non
assenza di difetti nel servizio: il gate rileva le difformita e resta rosso.

Due run consecutivi: 210 casi IT, 188 fallimenti, zero errori/skip; 18/18
status documentati osservati. Test ordinari: 229 passati anche senza Docker.

Aggiornamento normalizzazione: nuovo run con 239 test ordinari passati e
210 IT / 188 fallimenti / zero errori o skip. Dieci regressioni verificano alias
e body `Problem`; autoriferimento del parser eliminato. Il report ora mostra
le violazioni reali di `errors[0].element: null` prima nascoste dal resolver.

## T1: Provare la compatibilita MiniStack necessaria al servizio

**Priorita:** P0. **Dipendenze:** nessuna. **Taglia:** S/M.
**File probabili:** `src/test/java/.../rest/MinistackApiEnvironment.java`,
`src/test/resources/contracts/ministack-resources.json`.

**Descrizione:** verificare la release MiniStack scelta con gli SDK AWS del
progetto, prima di costruire il test HTTP. Decidere riuso e sostituzione delle
modifiche non committate; non cancellarle automaticamente.

**Accettazione:**
- [x] Immagine disponibile sull'architettura locale, versione/digest fissati,
  health check MiniStack raggiungibile e lifecycle confinato alla suite.
- [x] Create/Describe/Get/Update/Query DynamoDB e aggiornamenti condizionali
  usati dal DAO funzionano; SQS Send/Receive/Delete e EventBridge PutEvents
  con routing su SQS funzionano con le fixture controllate.
- [x] Le incompatibilita sono riportate esplicitamente, senza fallback a
  LocalStack o mock Java.

**Verifica:** smoke di bootstrap tramite SDK e endpoint del container; esiti
pubblicati prima di T2. Il controllo HTTP MiniStack e quello documentato:
`GET /_ministack/health`.

## T2: Avviare il microservizio completo su una porta HTTP reale

**Priorita:** P0. **Dipendenze:** T1. **Taglia:** M.
**File probabili:** `src/test/java/.../rest/MinistackApiContractIT.java`,
`src/test/java/.../rest/MinistackApiEnvironment.java`,
`src/test/resources/application-api-contract.properties`, `pom.xml`.

**Descrizione:** attivare il contesto completo con `RANDOM_PORT`, risorse AWS
dedicate e configurazione dinamica dei client. Aggiungere il profilo Failsafe
che seleziona soltanto la nuova suite.

**Accettazione:**
- [x] Nessun mock Java per controller, service, mapper, DAO, producer o updater;
  readiness dell'applicazione e dei listener verificata con timeout.
- [x] Entrambe le famiglie di endpoint AWS puntano a MiniStack con credenziali
  sintetiche; nessun accesso AWS reale o reset di ambienti condivisi.
- [x] Il profilo `api-contract` non esegue gli IT LocalStack e il normale
  lifecycle unitario non richiede Docker.

**Verifica:** `./mvnw -Papi-contract verify` con smoke HTTP;
`./mvnw test` senza container AWS.

## Checkpoint A: dopo T1-T2

- [x] Compatibilita SDK e avvio completo provati, non solo health dell'emulatore.
- [x] Un HTTP reale raggiunge il microservizio; logs e cleanup sono disponibili.
- [x] Proxy locale approvato per supplire alla fault injection non documentata.

## T3: Verificare i due GET contro OpenAPI

**Priorita:** P1. **Dipendenze:** T2. **Taglia:** M.
**File probabili:** `src/test/java/.../rest/MinistackApiContractIT.java`,
`src/test/java/.../rest/MinistackFixtures.java`,
`src/test/resources/contracts/internal-api.json`.

**Descrizione:** introdurre validazione schema/media type e fixture DynamoDB
coerenti per destinatario e pagamento, usando solo HTTP per chiamare il servizio.

**Accettazione:**
- [x] Entrambe le GET verificano 200, 400 e 404 con schema completo e `Problem`;
  record assenti ed eliminati sono distinti nei casi.
- [x] Le rappresentazioni con zero, uno e due tentativi analogici e
  `partialCost` attraversano serializzazione reale.
- [x] Le fixture sono validate contro OpenAPI e i `$ref` non vengono ignorati.

**Verifica:** `./mvnw -Papi-contract verify`;
report con endpoint, status, fixture e difformita riscontrate.

## T4: Verificare l'accettazione dei due POST asincroni

**Priorita:** P1. **Dipendenze:** T3. **Taglia:** M.
**File probabili:** `src/test/java/.../rest/MinistackApiContractIT.java`,
`src/test/java/.../rest/MinistackFixtures.java`,
`src/test/java/.../rest/MinistackApiEnvironment.java`,
`src/test/resources/contracts/internal-api.json`.

**Descrizione:** provare invio SQS reale, risposta HTTP e completamento delle
fixture tramite consumer, senza trattare il 202 come risultato finale.

**Accettazione:**
- [x] Entrambi i POST verificano 202 `text/plain` e 400 `Problem`; pubblicazioni
  AWS causate da input invalidi sono rilevate come difformita, non ignorate.
- [x] L'attesa dell'outcome correlato usa timeout/polling; il costo legale
  inizializzato e poi leggibile via GET.
- [x] Per il POST informale si verificano outcome e preparazione pagamenti,
  senza aspettare un costo legale che quel flusso non crea.

**Verifica:** `./mvnw -Papi-contract verify`;
assenza di sleep fissi e diagnostica chiara su timeout.

## T5: Verificare invalidazione e rilettura dei costi

**Priorita:** P1. **Dipendenze:** T4. **Taglia:** S/M.
**File probabili:** `src/test/java/.../rest/MinistackApiContractIT.java`,
`src/test/java/.../rest/MinistackFixtures.java`.

**Descrizione:** completare la quinta operazione con costi analogici preinseriti,
invalidazione via PUT e conferma tramite GET.

**Accettazione:**
- [x] Verifica di 204 senza body, 400 e 404 contro la specifica, con segnalazioni.
- [x] Invalidazione di una o entrambe le fasi confermata dalla rilettura HTTP,
  non dall'asserzione che un metodo interno sia stato invocato.
- [x] Liste vuote/duplicate e ripetizioni hanno esito coerente con il contratto
  corrente o una segnalazione esplicita, non una regola inventata dal test.

**Verifica:** `./mvnw -Papi-contract verify`.

## Checkpoint B: dopo T3-T5

- [x] Tutte le cinque operazioni sono raggiunte attraverso HTTP.
- [x] Percorsi positivi e 404 attraversano DAO/SDK; 400 verificati sul binding reale.
- [x] I POST non dipendono da una gara tra test e consumer per leggere SQS.

## T6: Aggiungere la matrice di robustezza degli input e delle risposte

**Priorita:** P1. **Dipendenze:** T5. **Taglia:** M.
**File probabili:** `src/test/java/.../rest/MinistackApiContractIT.java`,
`src/test/java/.../rest/InternalApiContractCases.java`,
`src/test/resources/contracts/internal-api.json`.

**Descrizione:** riutilizzare gli elementi schema/fixture condivisi e verificarli
sull'applicazione reale.

**Accettazione:**
- [x] Required, null, tipi, pattern, enum e limiti numerici/cardinalita coperti
  anche negli annidamenti, con i valori elencati nel piano e casi validi ai limiti.
- [x] Body malformati, input media type non supportati e parametri invalidi
  verificati; effetti AWS di richieste invalide segnalati come fallimenti.
- [x] Schema completo, somme documentate e sensibilita a mutazioni delle
  risposte verificati; nuove operazioni o fixture mancanti fanno fallire il gate.

**Verifica:** `./mvnw -Papi-contract verify`;
controesempi controllati devono essere rilevati dal validatore e ripristinati.

## T7: Provare i 500 con fault AWS localizzate

**Priorita:** P1. **Dipendenze:** T6. **Taglia:** M.
**File probabili:** `src/test/java/.../rest/MinistackApiEnvironment.java`,
`src/test/java/.../rest/MinistackApiContractIT.java`,
eventuale `src/test/java/.../rest/MinistackFaults.java`.

**Descrizione:** introdurre guasti ripetibili sul backend della richiesta senza
mock Java o cambi di codice di produzione. Se serve un proxy, valutarlo e
approvarlo prima di aggiungere una nuova dipendenza.

**Accettazione:**
- [x] Entrambe le GET, entrambi i POST e il PUT esercitano 500 attraverso SDK
  e HTTP reali, distinguendo errori di lettura, pubblicazione e aggiornamento.
- [x] Retry/timeouts hanno un limite finito e la fault non colpisce risorse di
  altri scenari; per PUT la fault deve raggiungere l'aggiornamento, non soltanto
  il controllo preliminare di esistenza.
- [x] Backend ripristinato e operazione HTTP nuovamente riuscita; niente failure
  post-202 interpretate come un 500 HTTP della richiesta precedente.

**Verifica:** `./mvnw -Papi-contract verify`;
matrice finale delle 18 coppie operazione/status e logs delle fault.

## T8: Documentare il comando locale e l'esito del gate

**Priorita:** P2. **Dipendenze:** T7. **Taglia:** S/M.
**File probabili:** `README.md`, `pom.xml`,
`src/test/java/.../rest/MinistackApiContractIT.java`.

**Descrizione:** rendere la suite riproducibile e chiarire i difetti riscontrati,
senza estendere il lavoro a una migrazione generale dei test o alla CI.

**Accettazione:**
- [x] Un unico comando `./mvnw -Papi-contract verify` prepara, esegue, riporta
  e ripulisce l'ambiente; funziona anche una seconda esecuzione consecutiva.
- [x] Report con casi coperti, fallimenti, limiti dell'emulatore e istruzioni
  di diagnosi; exit code non zero per difformita reali, niente test skipped.
- [x] Validatore/fixture condivisi conservati per MiniStack e regressioni;
  suite WebFlux isolata successivamente rimossa su richiesta. Nessun segreto
  o dipendenza da profili personali nei file.

**Verifica:** due esecuzioni isolate del comando di integrazione,
`./mvnw test`, revisione del diff e dei report.

## Checkpoint finale

- [x] Cinque operazioni e 18 coppie operazione/status esercitate.
- [x] Casi limite elencati coperti; ogni limite residuo e motivato.
- [x] Readiness, isolamento, fault, diagnostica e cleanup verificati.
- [ ] Owner API decide come rientrare dai test rossi senza indebolire il gate.

## Rientro delle difformita

Task proposti dal [report del 16 novembre 2026](../docs/report-contratto-api-interno.md).
Le attivita seguenti sono aperte e distinte dalle verifiche T1-T8 completate.
P1/P2 indicano l'ordine relativo del rientro, non la gravita di incidenti.

Verifica comune **V**: `./mvnw -Papi-contract verify`, esame dei casi interessati
in Failsafe e delle osservazioni HTTP. Il comando resta non-zero finche esistono
altre difformita; un task si chiude sui propri criteri, senza nascondere i residui.
I percorsi candidati sotto sono punti da verificare, non cause gia dimostrate.

## R1

**Errori HTTP conformi a Problem. Priorita:** P1. **Dipendenze:** nessuna.
**Taglia:** M.

Correggere media type e serializzazione delle proprieta opzionali degli errori.
**File candidati:** `PnResponseEntityExceptionHandlerActivation.java`,
`PnErrorWebExceptionHandlerActivation.java`, configurazione Jackson e test REST.
Gli handler sono in `src/main/java/.../config/springbootcfg/`.
Verificare il contributo di `pn-commons` prima di scegliere l'intervento.

**Accettazione:**
- [ ] Le risposte 400/404/500 documentate hanno media type `application/problem+json`.
- [ ] Tutti i body Problem osservati rispettano lo schema; `element` assente o
  stringa valida, mai null; `status` coerente con HTTP.
- [ ] Regressioni su errore di binding, not-found e fault AWS, senza soppressioni
  del validatore o alterazione degli status di errori autenticamente interni.

**Verifica:** V e `./mvnw -Dtest=InternalApiContractCasesTest test`.

## R2

**Dettaglio analogico opzionale conforme. Priorita:** P2. **Dipendenze:** nessuna.
**Taglia:** S/M.

Rendere conformi le GET senza componenti analogiche, nel totale e nel parziale.
**File candidati:** `service/mapper/NotificationDeliveryCostMapper.java`,
configurazione di serializzazione e test REST, sotto `src/main/java/...` e
`src/test/java/...`.

**Accettazione:**
- [ ] Risolte le 21 osservazioni sul totale e quella sul parziale con dettaglio null.
- [ ] I dettagli assenti sono omessi secondo contratto; nessuna modifica globale
  elimina campi required o componenti analogiche realmente presenti.
- [ ] I casi con zero, uno e due tentativi restano validati integralmente.

**Verifica:** V; confronto GET destinatario/pagamento digitali e analogiche.

## R3

**Preservare productType durante l'invalidazione. Priorita:** P1.
**Dipendenze:** nessuna. **Taglia:** M.

Tracciare aggiornamento e rilettura per conservare un attributo richiesto valido.
**File candidati:** `rest/NotificationCostServiceController.java`,
`service/mapper/NotificationCostUpdaterMapper.java`, updater/DAO effettivamente
responsabile e test REST. Limitare la correzione al percorso dimostrato.

**Accettazione:**
- [ ] GET successive all'invalidazione espongono `productType` valido per entrambi
  i tentativi; risolte le sei osservazioni per ciascuna posizione.
- [ ] Non si inventano valori di default e non si omette la proprieta required.
- [ ] Costo invalidato e fasi non coinvolte mantengono il comportamento previsto.

**Verifica:** V; invalidazione di una e di entrambe le fasi, poi rilettura HTTP.

## R4

**Rifiutare i body POST non conformi. Priorita:** P1. **Dipendenze:** nessuna.
**Taglia:** M.

Impedire che coercizioni e null non ammessi mascherino il JSON ricevuto.
**File candidati:** configurazione Jackson/binding, generazione DTO in `pom.xml`,
specifica di input se serve correggere la generazione, test REST.
Non modificare manualmente i DTO generati.

**Accettazione:**
- [ ] Gli 11 casi legali e i cinque informali del report ricevono 400, non 202.
- [ ] Nessuna pubblicazione SQS per questi input; i tipi corretti e i limiti
  validi restano accettati.
- [ ] `payments` legale puo essere omesso come previsto, ma il null esplicito
  non viene equiparato automaticamente all'omissione.

**Verifica:** V; confronto degli stessi 16 scenari e degli effetti applicativi.

## R5

**Rifiutare elementi null negli array. Priorita:** P1. **Dipendenze:** nessuna.
**Taglia:** M.

Applicare la validazione agli elementi e agli oggetti annidati prima dell'uso.
**File candidati:** configurazione di generazione/binding, controller se
necessario, specifica come sorgente dei DTO e test REST.

**Accettazione:**
- [ ] `costRecipients[0]` e `payments[0]` null nei due POST ricevono 400.
- [ ] `costPhases[0]` null nel PUT riceve 400.
- [ ] Nessun effetto applicativo per questi input e nessuna conversione generica
  degli errori interni in errori del client.

**Verifica:** V; i cinque scenari passano le asserzioni sullo status e sugli effetti.

## Checkpoint rientro A

- [ ] R1, R4 e R5 chiusi: errori conformi e input POST invalidi rifiutati.
- [ ] Gli altri fallimenti restano visibili, senza esclusioni o assert rimossi.

## R6

**Far rispettare costPhases required. Priorita:** P1. **Dipendenze:** nessuna.
**Taglia:** S/M.

Distinguere proprieta assente e array esplicitamente vuoto nel PUT.
**File candidati:** generazione di `PaperCostToInvalidateDto` in `pom.xml`,
binding del controller e test REST; DTO generati esclusi dagli edit manuali.

**Accettazione:**
- [ ] PUT senza `costPhases` riceve 400 senza invocare aggiornamenti.
- [ ] Valore null rifiutato; array vuoto e duplicati trattati secondo il contratto,
  senza aggiungere arbitrariamente `minItems` o `uniqueItems`.
- [ ] Richieste valide continuano a produrre 204 vuoto e gli effetti attesi.

**Verifica:** V; casi missing/null/empty e invalidazioni valide.

## Checkpoint rientro B

- [ ] R6 e R3 chiusi: validazione e rappresentazione post-invalidazione conformi.
- [ ] Nessuna regressione su lettura, fasi non invalidate e fault UpdateItem.

## R7

**Allineare negoziazione e contratto. Priorita:** P2. **Dipendenze:** R1.
**Taglia:** M.

Ottenere la decisione dell'owner su 406/415 e allineare risposta e documentazione.
**File candidati:** `docs/openapi/api-private.yaml`, configurazione/handler HTTP,
`MinistackApiContractIT.java` e fixture di contratto.

**Accettazione:**
- [ ] Decisione approvata per Accept non supportato e Content-Type non supportato.
- [ ] Tutti gli otto scenari hanno status, header e body coerenti con il contratto
  approvato; le eventuali nuove coppie entrano nella matrice.
- [ ] L'eventuale modifica OpenAPI supera il validatore Spectral con profilo
  concordato o riporta esplicitamente i residui preesistenti, senza nuovi rilievi.

**Verifica:** V, riesame delle cinque risposte 406 e tre 415 e validazione OpenAPI
secondo la procedura Spectral di prodotto.

## R8

**Concordare la semantica economica. Priorita:** P1. **Dipendenze:** nessuna.
**Taglia:** S, decisione/documentazione.

L'owner API definisce se i dettagli rappresentano costi sostenuti o addebitabili
e come `FLAT_RATE`, `DELIVERY_MODE` e `applyCost` influenzano totale e parziale.
**File candidati:** descrizioni in `docs/openapi/api-private.yaml` e documentazione
della decisione nel presente piano o nel report.

**Accettazione:**
- [ ] Tabella approvata per entrambe le politiche e applyCost vero/falso dove
  applicabile, distinguendo GET destinatario e GET pagamento.
- [ ] Definiti significato, IVA e somme di totale, parziale e componenti;
  spiegati i quattro scenari oggi contraddittori.
- [ ] Registrata la valutazione di compatibilita per i consumer, senza assumere
  che il risultato corrente o la somma del test siano sempre corretti.

**Verifica:** revisione dell'owner su esempi numerici riproducibili, inclusi
i 333/250 eurocent delle fixture; nessuna modifica del calcolo in questo task.

## R9

**Applicare la decisione economica approvata. Priorita:** P1.
**Dipendenze:** R8. **Taglia:** M.

Allineare soltanto le parti risultate errate: calcolo, mapper o descrizioni.
**File candidati:** `service/impl/CostCalculatorImpl.java`,
`service/mapper/NotificationDeliveryCostMapper.java`, specifica e test economici.

**Accettazione:**
- [ ] Le combinazioni approvate in R8 hanno valori e dettagli coerenti.
- [ ] Test economici verificano regole effettive ed eccezioni esplicite: non
  eliminare indiscriminatamente le asserzioni di somma.
- [ ] Il caso DELIVERY_MODE/applyCost=true e gli altri percorsi corretti non
  regrediscono; modifiche OpenAPI validate anche con Spectral.

**Verifica:** test mirati del calcolatore/mapper interessato, V e controllo
dei quattro scenari economici oggi falliti.

## R10

**Chiudere il gate e aggiornare le evidenze. Priorita:** P1.
**Dipendenze:** R1, R2, R3, R4, R5, R6, R7, R9. **Taglia:** S.

Consolidare il rientro senza cambiare arbitrariamente il livello di verifica.
**File candidati:** report leggibile, `README.md`, presente checklist e piano;
nessun cambiamento produttivo previsto.

**Accettazione:**
- [ ] `./mvnw test` passa senza runtime container; V passa con runtime disponibile.
- [ ] Nessun failure/error/skip; 18 coppie originarie conformi e nuove eventuali
  coppie coperte; tutti i test di robustezza ed effetti passano senza riduzioni.
- [ ] Report aggiornato con risultati, decisioni approvate e limiti residui;
  nessun riferimento circolare del validatore.

**Verifica:** comandi sopra, revisione di Failsafe/matrice/osservazioni e diff;
non basta avere `schemaConformant: true` nella sola matrice.
