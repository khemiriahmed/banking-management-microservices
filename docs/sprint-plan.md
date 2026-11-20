# Feuille de route des sprints

Objectif : un sprint traité par jour ouvré. Les sprints longs du cahier des charges sont découpés en livrables quotidiens ; un sprint n'est clos que lorsque ses critères de validation sont passés.

| Jour | Sprint | Résultat principal | Dépendance | Statut |
| --- | --- | --- | --- | --- |
| J1 | 0 — Préparation | Outils, dépôt, structure, conventions Git | — | Terminé |
| J2 | 1 — Fondations Spring Cloud | Config Server + Eureka + Gateway | S0 | Terminé |
| J3 | 2 — Auth | Inscription, login, JWT via Gateway | S1 | À faire |
| J4 | 3 — Users | Profil client sécurisé, rôles | S2 | À faire |
| J5 | 4 — Accounts | Comptes, soldes `BigDecimal`, statuts | S3 | À faire |
| J6 | 5 — Beneficiaries | CRUD bénéficiaires isolés par client | S4 | À faire |
| J7-8 | 6 — Transfers | Saga, compensation, idempotence | S4-S5 | À faire |
| J9 | 7 — Transactions | Historique paginé et sécurisé | S6 | À faire |
| J10 | 8 — Events | RabbitMQ, notifications, audit | S6-S7 | À faire |
| J11-12 | 9 — Angular | Parcours CLIENT et ADMIN complets | APIs stables | À faire |
| J13 | 10 — Docker | `docker compose up --build` | S9 | À faire |
| J14 | 11 — CI/CD | GitHub Actions, OpenAPI, README final | S10 | À faire |
| J15 | 12 — AWS | Démo sécurisée, coûts documentés | S11 | À faire |

## Détail du Sprint 0 (terminé)

- [x] Lecture et analyse du cahier des charges.
- [x] 0.1 — Vérifier les outils : Java 17, Maven, Node 18, npm, Docker.
- [x] 0.2 — Définir les dossiers du monorepo.
- [x] 0.3 — README, `.gitignore`, conventions de branches et de commits.
- [x] 0.4 — Documentation initiale et diagramme d'architecture.
- [x] 0.5 — Premier commit poussé sur GitHub (`develop` et `main`).

### État des prérequis

| Outil | Attendu | Observé | Action |
| --- | --- | --- | --- |
| Java | 17 | 26.0.1 global, Temurin 17.0.21 portable | Toolchain 17 utilisé pour les builds Sprint 1 |
| Maven | 3.9+ | absent globalement, 3.9.16 portable | Maven Wrapper ajouté dans chaque service |
| Node | 18.x (Angular 15) | 24.14.1 | Installer Node 18 LTS avant le Sprint 9 |
| npm | 9+ | 10.9.8 (`npm.cmd`) | Utiliser `npm.cmd` si PowerShell bloque `npm.ps1` |
| Docker | présent | 29.3.1 | OK |

## Détail du Sprint 1 (terminé)

- [x] 1.1 — Config Server : dépendance, `@EnableConfigServer`, port 8888, profil `native`, tests.
- [x] 1.2 — Configuration externe non sensible dans `infrastructure/config-repo/`.
- [x] 1.3 — Discovery Server : `@EnableEurekaServer`, port 8761, tests.
- [x] 1.4 — API Gateway : dépendance WebFlux, Eureka client, import Config Server, tests.
- [x] 1.5 — Route de test `demo-service` via `lb://DEMO-SERVICE`.
- [x] 1.6 — Démarrage complet et vérification des endpoints (voir [ports et services](ports-and-services.md)).

Commits atomiques : un commit par tâche sur la branche de sprint dédiée, puis merge `--no-ff` dans `develop`. La branche du sprint est conservée (convention : `sprint/01-foundations`).

## Règle de passage au sprint suivant

Aucun sprint suivant ne démarre tant que les critères de validation du sprint courant ne sont pas tous cochés et que le merge dans `develop` n'est pas effectué.
