# Framework MVC Mini

Un framework MVC léger pour applications web Java, développé avec Jakarta Servlet 6.0.

## 🚀 Démarrage Rapide

### Prérequis
- Java 17+
- Maven 3.6+
- Serveur compatible Jakarta Servlet 6.0 (Tomcat 10+, Jetty 11+)

### Installation

```bash
mvn clean install
```

### Configuration

Créez un fichier de configuration dans `/WEB-INF/config/application.properties` :

```properties
jsp_base_path=/WEB-INF/views
upload_path=/uploads
```

## 📚 Documentation Complète

Pour voir **toutes les fonctionnalités implémentées**, consultez le fichier **[FEATURES.md](FEATURES.md)**.

Ce document contient :
- ✅ Architecture MVC complète
- ✅ Système de routage dynamique
- ✅ Annotations (@Controller, @GetMapping, @PostMapping, @JsonUrl, etc.)
- ✅ Gestion des paramètres et binding automatique
- ✅ Upload de fichiers
- ✅ Rendu de contenu (String, ModelView, JSON)
- ✅ Configuration centralisée
- ✅ Système de logging
- ✅ Utilitaires JSON, Date, DataType

## 🎯 Exemple Rapide

```java
@Controller(mapping = "/api/users")
public class UserController {

  @UrlMap("/{id}")
  @GetMapping
  @JsonUrl
  public User getUser(@UrlParam(name = "id") int id) {
    return userService.findById(id);
  }

  @UrlMap("/")
  @PostMapping
  public String createUser(User user) {
    userService.save(user);
    return "User created!";
  }
}
```

## 🏗️ Structure du Projet

```
src/main/java/mg/miniframework/
├── annotation/          # Annotations du framework
├── controller/          # Front Controller Servlet
├── modules/             # Modules principaux (ModelView, RouteMap, etc.)
└── utils/               # Utilitaires (JSON, Date, DataType)
```

## 🔧 Fonctionnalités Principales

### Routage
- Routes dynamiques avec paramètres : `/users/{id}`
- Support GET et POST
- Matching automatique des URLs

### Injection de Paramètres
- Paramètres d'URL (`@UrlParam`)
- Paramètres de formulaire (`@RequestAttribute`)
- Binding automatique d'objets
- Support des objets imbriqués

### Rendu
- Vues JSP avec ModelView
- JSON automatique avec `@JsonUrl`
- Texte/HTML direct avec String

### Upload de Fichiers
- Support multipart/form-data
- Sauvegarde atomique
- Injection via `Map<Path, File>`

### Logging
- Logs structurés avec niveaux (ERROR, WARN, INFO, DEBUG, SUCCESS)
- Fichiers journaliers automatiques
- Répertoire configurable

## 📦 Dépendances

Le framework ne nécessite qu'une seule dépendance :

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
```

## 🛠️ Build

```bash
# Compiler
mvn compile

# Packager en JAR
mvn package
```

## 📝 License

Ce projet est un framework éducatif pour l'apprentissage du pattern MVC.

## 👥 Contribution

Contributions bienvenues ! Voir [FEATURES.md](FEATURES.md) pour la liste complète des fonctionnalités existantes avant de proposer des améliorations.

---

**Version** : 1.0.0  
**Auteur** : Framework S5 Team  
**Java** : 17+  
**Servlet API** : Jakarta 6.0
