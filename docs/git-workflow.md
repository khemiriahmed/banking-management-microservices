# Convention Git et rituel quotidien

## Branches

| Branche | Rôle | Depuis | Vers |
| --- | --- | --- | --- |
| `main` | État Stable / releases | `develop` | — |
| `develop` | Intégration continue du projet | `main` | `main` |
| `feature/sprint-XX-slug` | Une tâche ou un lot cohérent de tâches | `develop` | `develop` |
| `fix/slug` | Correction ciblée | `develop` | `develop` |
| `docs/slug` | Documentation seule | `develop` | `develop` |
| `chore/slug` | Outillage, configuration, dépendances | `develop` | `develop` |

Règles : jamais de commit direct sur `main`, pas de branch qui vit plus d'une journée de travail, une branche = un objectif.

## Messages de commit

Format : `type(portee): description impérative courte`

Types autorisés : `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `build`, `ci`.

Exemples :

```text
feat(auth): register client with bcrypt password
test(account): reject suspended account debit
docs(sprint-0): add architecture overview
```

## Rituel quotidien (1 sprint = 1 jour)

```powershell
git switch develop
git pull origin develop
git switch -c feature/sprint-03-user-service
# développement : une tâche, un commit, une vérification
git status
git add <fichiers>
git commit -m "feat(user): expose GET /users/me"
# vérifications locales obligatoires avant merge
git switch develop
git merge --no-ff feature/sprint-03-user-service -m "Merge feature/sprint-03-user-service into develop"
git branch -d feature/sprint-03-user-service
git push origin develop
```

Si le sprint dure plusieurs jours, créer une nouvelle branche `feature/sprint-XX-...` par tranche de tâches et merger à chaque fin de journée.

## Definition of Done avant merge

- [ ] Le code compile et respecte le découpage du service.
- [ ] Les migrations Flyway sont versionnées et reproductibles.
- [ ] Les règles métier et autorisations sont testées.
- [ ] Les erreurs sont gérées et les logs ne révèlent aucun secret.
- [ ] Le README du service précise rôle, port, configuration, endpoints et commandes de test.
- [ ] Les tests passent localement.

## Secrets

`.env`, `.env.*`, `*.pem`, `*.key` sont ignorés. Seul `.env.example` sans valeur sensible peut être commité. Aucun token AWS, mot de passe ou JWT réel ne doit apparaître dans Git.
