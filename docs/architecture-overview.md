# Vue d'architecture — Banking Management System

## Principes

1. Un point d'entrée public : l'API Gateway. Le frontend n'appelle jamais directement un service métier.
2. Découverte dynamique via Eureka ; configuration non sensible centralisée via Spring Cloud Config.
3. Base de données privée par service. Aucune jointure ni accès SQL direct entre services.
4. Communications synchrones via REST interne (identité propagée par JWT validé), asynchrones via RabbitMQ à partir du sprint 8.
5. Sécurité défensive : chaque service revérifie le token, le rôle et la propriété de la ressource.

## Flux d'une requête

```text
Navigateur
  -> API Gateway (JWT, routage, rate limiting futur)
  -> Service métier (validation JWT + rôle + propriété)
  -> Base dédiée
```

Le Gateway transmet l'en-tête `Authorization`. Les services ne font pas confiance à un simple en-tête `X-User-Id` venant du client.

## Périmètre des services

| Service | Responsabilité | Base | Sprint |
| --- | --- | --- | --- |
| config-server | Configuration centralisée non sensible | — | 1 |
| discovery-server | Registre Eureka | — | 1 |
| api-gateway | Routage et point d'entrée unique | — | 1 |
| auth-service | Identifiants, rôles, BCrypt, JWT | auth_db | 2 |
| user-service | Profils clients, administration des utilisateurs | user_db | 3 |
| account-service | Comptes, soldes, statuts, bénéficiaires | account_db | 4-5 |
| transfer-service | Virements, Saga, idempotence | transfer_db | 6 |
| transaction-service | Historique des mouvements confirmés | transaction_db | 7 |
| notification-service | Notifications via événements | notification_db | 8 |
| audit-service | Traçabilité des actions sensibles | audit_db | 8 |

## Règles métier transversales

- Identifiants `UUID` pour les entités métier.
- Montants et soldes : `BigDecimal`, devise explicite, jamais `float`/`double`.
- Statuts de compte : actif, suspendu, clôturé ; un compte suspendu ne peut pas être débité.
- Virements : `REQUESTED -> VALIDATING -> PROCESSING -> COMPLETED | FAILED`, compensation en cas d'échec.
- Idempotence : clé unique stockée avec le résultat pour éviter les doubles débits.
- Événements : `eventId`, `eventType`, `occurredAt`, payload minimal, sans secret ni token.

## Règles de non-régression

- Pas de Kubernetes, Kafka ou outil supplémentaire avant la version Docker Compose fonctionnelle.
- Chaque migration Flyway est versionnée et reproductible.
- Les logs ne contiennent ni mot de passe, ni JWT, ni données personnelles.
- L'environnement de démonstration AWS reste privé pour la base et public uniquement via le Gateway.
