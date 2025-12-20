# Fonctionnalités à implémenter prochainement

Ce document complète `FEATURES.md` en listant les évolutions prioritaires pour faire passer `framework_s5` d'un MVP fonctionnel à un framework plus complet et robuste.

## 1. Routage & HTTP avancés
- **Support PUT / DELETE / PATCH / OPTIONS** : `Url.Method` expose déjà ces verbes mais ils ne sont pas câblés. Ajouter les annotations correspondantes (`@PutMapping`, `@DeleteMapping`, …) et leur résolution côté `RouteMap`/`FrontControllerServlet` permettrait de couvrir les cas REST complets.
- **Contraintes sur les paramètres de chemin** : autoriser des regex par segment (`/users/{id:\\d+}`) pour éviter les collisions et offrir une validation précoce.
- **Versioning et regroupement des routes** : permettre des préfixes (`/api/v1`) ou des registres séparés afin de faciliter la cohabitation de plusieurs versions d'API.

## 2. Injection de dépendances & configuration
- **Mini container IoC** : instancier les contrôleurs et services via un registre partagé pour supporter les singletons, l'injection par constructeur et la réutilisation des ressources (connexion BD, services). Aujourd'hui chaque requête crée un nouveau contrôleur.
- **Gestion typée de la configuration** : exposer une API type `Config.get("datasource.url")` avec conversion automatique (booléens, entiers, listes) et profils (`dev`, `test`, `prod`) rechargés à chaud.

## 3. Validation & sécurité
- **Annotations de validation** (`@NotNull`, `@Email`, `@Size`, etc.) appliquées avant l'invocation du contrôleur, avec construction automatique des messages d'erreur et d'un `ModelView`/JSON 400.
- **Gestion centralisée de l'authentification / autorisation** : hooks ou annotations (`@RequireRole("ADMIN")`) pour vérifier les droits avant l'exécution d'une méthode.
- **Protection CSRF et sécurisation des fichiers uploadés** : vérifier les extensions, la taille, et générer des noms temporaires pour éviter les collisions.

## 4. Middleware & gestion globale des erreurs
- **Chaîne de filtres/intercepteurs** (avant/après contrôleur) pour ajouter facilement du logging, du tracing ou de la transformation de requête sans modifier les contrôleurs.
- **Gestionnaire d'exceptions global** : mapper automatiquement des exceptions personnalisées vers des réponses (JSON ou vues) avec les bons codes HTTP.
- **Instrumentation / tracing (partiellement implémenté)** : métriques de base (requêtes, erreurs, durée moyenne) exportées via endpoint `/metrics` au format Prometheus.

## 5. Réponses & rendu
- **Builder de réponse** façon `ResponseEntity` : définir statut, en-têtes et corps explicitement, quel que soit le type de sortie (HTML/JSON/fichier binaire).
- **Système de templates amélioré** : support des layouts/partials (via JSP tag files ou un moteur comme Thymeleaf/Freemarker) pour factoriser les vues.
- **Streaming & download de fichiers** : API simplifiée pour retourner un `InputStream` ou un fichier avec le bon `Content-Type`.

## 6. Expérience développeur
- **CLI de scaffolding** : génération d'un squelette de contrôleur, routes et vues pour accélérer le démarrage d'un nouveau module.
- **Hot reload côté serveur** : relire automatiquement la configuration et recharger les routes sans redéployer l'application complète.
- **Documentation automatique** : produire un export (OpenAPI/HTML) listant routes, paramètres, types de retour à partir des annotations existantes.

Ces chantiers peuvent être priorisés en fonction des besoins du projet (API REST, sécurité, industrialisation). Chaque section ci-dessus fournit un point de départ concret pour planifier les prochaines itérations du framework.