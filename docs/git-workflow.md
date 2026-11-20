# Convention Git et rituel quotidien

## Branches

| Branche | Rôle | Depuis | Vers |
| --- | --- | --- | --- |
| `main` | État Stable / releases | `develop` | — |
| `develop` | Intégration continue du projet | `main` | `main` |
| `sprint/XX-slug` | Une branche par sprint, conservée après merge | `develop` | `develop` |
| `feature/sprint-XX-slug` | Sous-branche pour une tâche complexe du sprint | `sprint/XX-slug` | `sprint/XX-slug` |
| `fix/slug` | Correction ciblée | `develop` | `develop` |
| `docs/slug` | Documentation seule | `develop` | `develop` |
| `chore/slug` | Outillage, configuration, dépendances | `develop` | `develop` |

Règles : **un sprint = une branche dédiée**, créée depuis `develop` et conservée sur GitHub après le merge. Les commits sont atomiques dans cette branche. Jamais de commit direct sur `main` ni sur `develop`. Les sous-branches `feature/...` sont supprimées après merge dans la branche du sprint.

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
git switch -c sprint/03-user-service
# développement : une tâche, un commit, une vérification
git status
git add <fichiers>
git commit -m "feat(user): expose GET /users/me"
# vérifications locales obligatoires avant merge
git switch develop
git merge --no-ff sprint/03-user-service -m "Merge sprint/03-user-service into develop"
git push origin develop sprint/03-user-service
```

La branche `sprint/03-user-service` reste disponible sur GitHub pour l'historique et les reprises. Si une tâche est trop volumineuse, créer une sous-branche `feature/sprint-03-jwt` depuis `sprint/03-user-service`, la merger dans cette dernière, puis la supprimer.

## Definition of Done avant merge

- [ ] Le code compile et respecte le découpage du service.
- [ ] Les migrations Flyway sont versionnées et reproductibles.
- [ ] Les règles métier et autorisations sont testées.
- [ ] Les erreurs sont gérées et les logs ne révèlent aucun secret.
- [ ] Le README du service précise rôle, port, configuration, endpoints et commandes de test.
- [ ] Les tests passent localement.

## Secrets

`.env`, `.env.*`, `*.pem`, `*.key` sont ignorés. Seul `.env.example` sans valeur sensible peut être commité. Aucun token AWS, mot de passe ou JWT réel ne doit apparaître dans Git.
