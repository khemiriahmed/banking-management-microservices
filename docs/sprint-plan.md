# Feuille de route des sprints

Objectif : un sprint traité par jour ouvré. Les sprints longs du cahier des charges sont découpés en livrables quotidiens ; un sprint n'est clos que lorsque ses critères de validation sont passés.

| Jour | Sprint | Résultat principal | Dépendance | Statut |
| --- | --- | --- | --- | --- |
| J1 | 0 — Préparation | Outils, dépôt, structure, conventions Git | — | En cours |
| J2 | 1 — Fondations Spring Cloud | Config Server + Eureka + Gateway | S0 | À faire |
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

## Détail du Sprint 0 (en cours)

- [x] Lecture et analyse du cahier des charges.
- [ ] 0.1 — Vérifier les outils : Java 17, Maven, Node 18, npm, Docker.
- [x] 0.2 — Définir les dossiers du monorepo.
- [x] 0.3 — README, `.gitignore`, conventions de branches et de commits.
- [x] 0.4 — Documentation initiale et diagramme d'architecture.
- [ ] 0.5 — Premier commit poussé sur GitHub (en attente d'accord pour `git push`).

### Blocage prérequis de la machine de développement

| Outil | Attendu | Observé | Action |
| --- | --- | --- | --- |
| Java | 17 | 26.0.1 | Installer une JDK 17 (Temurin) et la sélectionner via `JAVA_HOME` |
| Maven | 3.9+ | absent | Installer Maven ou utiliser un Maven Wrapper |
| Node | 18.x (Angular 15) | 24.14.1 | Installer Node 18 LTS |
| npm | 9+ | 10.9.8 (`npm.cmd`) | Utiliser `npm.cmd` si PowerShell bloque `npm.ps1` |
| Docker | présent | 29.3.1 | OK |

## Règle de passage au sprint suivant

Aucun sprint suivant ne démarre tant que les critères de validation du sprint courant ne sont pas tous cochés et que le merge dans `develop` n'est pas effectué.
