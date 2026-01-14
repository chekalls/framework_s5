# Fonctionnalités Implémentées - Framework MVC Mini

Ce document liste toutes les fonctionnalités déjà implémentées dans le framework MVC mini.

## 📋 Table des matières

1. [Architecture MVC](#architecture-mvc)
2. [Système de Routage](#système-de-routage)
3. [Annotations](#annotations)
4. [Gestion des Paramètres](#gestion-des-paramètres)
5. [Gestion des Fichiers](#gestion-des-fichiers)
6. [Rendu de Contenu](#rendu-de-contenu)
7. [Configuration](#configuration)
8. [Logging](#logging)
9. [Utilitaires](#utilitaires)

---

## 🏗️ Architecture MVC

### Front Controller Servlet
- **FrontControllerServlet** : Servlet principal qui intercepte toutes les requêtes (`/*`)
- Pattern Front Controller pour centraliser la gestion des requêtes HTTP
- Support des ressources statiques (CSS, JS, images, JSP)
- Gestion automatique des erreurs 404 avec liste des routes disponibles
- Support multipart pour l'upload de fichiers (`@MultipartConfig`)

---

## 🛣️ Système de Routage

### Routage Dynamique
- **RouteMap** : Mappage automatique des URLs vers les méthodes
- Support des patterns d'URL avec paramètres dynamiques : `/users/{id}`, `/posts/{id}/comments/{commentId}`
- Extraction automatique des paramètres d'URL
- Distinction GET/POST via annotations

### Matching de Routes
- **RoutePatternUtils** : Conversion des patterns en expressions régulières
- Matching précis des URLs avec paramètres
- Extraction et injection automatique des paramètres de route

---

## 🏷️ Annotations

### Annotations de Contrôleur
- **@Controller** : Marque une classe comme contrôleur
  - `mapping()` : Préfixe d'URL pour toutes les routes du contrôleur
  - Exemple : `@Controller(mapping = "/users")`

### Annotations de Méthode
- **@UrlMap** : Associe une méthode à une URL
  - `value()` : Pattern de l'URL
  - Exemple : `@UrlMap("/{id}")`

- **@GetMapping** : Spécifie une route HTTP GET
  - Utilisé avec `@UrlMap`

- **@PostMapping** : Spécifie une route HTTP POST
  - Utilisé avec `@UrlMap`

- **@JsonUrl** : Retourne automatiquement du JSON
  - Convertit le résultat de la méthode en JSON
  - Définit le Content-Type à `application/json`

### Annotations de Paramètres
- **@UrlParam** : Injection de paramètres d'URL
  - `name()` : Nom du paramètre dans l'URL
  - Exemple : `@UrlParam(name = "id") String userId`

- **@RequestAttribute** : Injection de paramètres de requête/formulaire
  - `paramName()` : Nom du paramètre dans la requête
  - `defaultValue()` : Valeur par défaut si absent
  - Exemple : `@RequestAttribute(paramName = "name", defaultValue = "")`

- **@FormParam** : Annotation pour les paramètres de formulaire
  - `name()` : Nom du champ du formulaire

- **@Route** : Annotation générique de route

---

## 📥 Gestion des Paramètres

### Injection Automatique
- **Paramètres primitifs** : Conversion automatique vers int, long, double, boolean, etc.
- **Chaînes de caractères** : Injection directe
- **Dates** : Support de Date, LocalDate, LocalDateTime
  - Format par défaut : `yyyy-MM-dd`
- **Objets personnalisés** : Binding automatique des attributs
  - Support de la notation pointée : `user.name`, `user.age`
  - Reconstruction automatique d'objets POJO

### Paramètres Complexes
- **Tableaux primitifs** : Support de tableaux avec notation `[index]`
  - Exemple : `users[0].name`, `users[1].name`
- **Listes d'objets** : Support des collections avec notation indexée
- **Maps** : Injection de Map<String, Object> avec paramètres de requête
  - Support de Map<Path, byte[]> pour les fichiers
  - Support de Map<Path, File> pour les fichiers avec métadonnées

### Objets Imbriqués
- Support des objets imbriqués avec notation pointée
- Reconstruction récursive des objets complexes
- Support des tableaux d'objets

---

## 📁 Gestion des Fichiers

### Upload de Fichiers
- **Support multipart/form-data** : Upload de fichiers via formulaires
- **Classe File** : Encapsulation des fichiers uploadés
  - `contextPath` : Chemin relatif dans le contexte web
  - `absolutePath` : Chemin absolu sur le système de fichiers
  - `content` : Contenu binaire du fichier
  - `save()` : Sauvegarde atomique avec fichier temporaire

### FileHandler
- **Sauvegarde atomique** : Utilisation de fichiers temporaires + move atomique
- **Création automatique** : Création des répertoires parents si nécessaires
- **Gestion d'erreurs** : Exceptions claires en cas d'échec

### Configuration Upload
- Chemin d'upload configurable via `upload_path` dans les properties
- Chemin par défaut : `/public`
- Support de l'encodage de caractères (UTF-8 par défaut)

---

## 🎨 Rendu de Contenu

### Types de Retour Supportés
1. **String** : Rendu direct en HTML/texte
   - Content-Type : `text/html;charset=UTF-8`

2. **ModelView** : Rendu JSP avec données
   - Forward vers une vue JSP
   - Injection des données dans le request scope
   - Chemin de base JSP configurable

3. **JSON** : Sérialisation automatique (avec @JsonUrl)
   - Conversion d'objets POJO en JSON
   - Support des Maps
   - Support des Listes
   - Échappement automatique des caractères spéciaux

4. **null** : Retourne un statut 204 No Content

### ContentRenderManager
- **Gestion centralisée** du rendu de contenu
- **Normalisation des chemins JSP** : Ajout automatique du `/` si absent
- **Base path configurable** : Via `jsp_base_path` dans la config
- **Conversion JSON** : JsonUtils intégré pour la sérialisation

---

## ⚙️ Configuration

### ConfigLoader
- **Lecture automatique** des fichiers properties
- **Emplacement** : `/WEB-INF/config/*.properties`
- **Chargement au démarrage** : Configuration chargée au premier appel
- **Mise en cache** : Configuration stockée dans ServletContext

### Paramètres Configurables
- `jsp_base_path` : Répertoire de base pour les vues JSP
- `upload_path` : Répertoire pour les fichiers uploadés
- Tous les paramètres sont accessibles via Map<String, String>

---

## 📊 Logging

### LogManager
- **Logs structurés** : Timestamp + Niveau + Message
- **Niveaux de log** :
  - ERROR : Erreurs
  - WARN : Avertissements
  - INFO : Informations
  - DEBUG : Débogage détaillé
  - SUCCESS : Opérations réussies

### Fonctionnalités
- **Fichiers journaliers** : Un fichier par jour (`log_YYYY-MM-DD.txt`)
- **Répertoire configurable** : Via propriété système `miniframework.logdir`
- **Répertoire par défaut** : `./log`
- **Création automatique** : Création du répertoire et fichiers si nécessaires
- **Logs de démarrage** : Logs automatiques des contrôleurs et config trouvés

---

## 🛠️ Utilitaires

### JsonUtils
- **Sérialisation JSON sans dépendance** : Implémentation manuelle
- **Support des types** :
  - Primitives (String, Number, Boolean)
  - Maps
  - Listes
  - POJOs (via réflexion)
- **Échappement** : Gestion automatique des caractères spéciaux (`"`, `\n`, `\r`, `\t`)

### DataTypeUtils
- **Conversion de types** : Conversion automatique entre types primitifs et wrappers
- **Détection de types** :
  - `isPrimitiveOrWrapper()` : Vérifie si un type est primitif
  - `isArrayType()` : Détecte les tableaux et collections
  - `getContentType()` : Extrait le type des éléments d'une collection
- **Conversion de collections** :
  - List vers Array
  - Array vers List
  - Support des types génériques

### DateUtils
- **Parsing de dates** : Conversion String vers Date/LocalDate/LocalDateTime
- **Format configurable** : Format par défaut `yyyy-MM-dd`
- **Support des formats modernes** : LocalDate et LocalDateTime (Java 8+)

### RoutePatternUtils
- **Conversion pattern → regex** : Transformation des patterns d'URL en expressions régulières
- **Extraction de paramètres** : Extraction automatique des valeurs depuis l'URL
- **Support des paramètres multiples** : Plusieurs paramètres dans une même URL

---

## 🔍 Autres Fonctionnalités

### Détection Automatique des Contrôleurs
- Scan du classpath au démarrage
- Détection via annotation `@Controller`
- Enregistrement automatique des routes

### Gestion des Erreurs
- **404 personnalisé** : Page d'erreur avec liste des routes disponibles
- **500 personnalisé** : Affichage du message d'erreur
- **Logs d'erreurs** : Toutes les exceptions sont loggées

### Support Servlet
- **Jakarta Servlet 6.0** : Version moderne de l'API Servlet
- **Scope provided** : Le conteneur fournit l'API
- **Compatible** : Tomcat 10+, Jetty 11+

### Statuts de Route
- **RouteStatus** : Énumération des codes de retour
  - NOT_FOUND : Route non trouvée
  - RETURN_JSON : Retour JSON
  - RETURN_STRING : Retour texte
  - RETURN_MODEL_VIEW : Retour vue JSP
  - RETURN_TYPE_UNKNOWN : Type de retour non géré

---

## 📦 Structure du Projet

```
mg.miniframework/
├── annotation/          # Annotations du framework
│   ├── Controller.java
│   ├── GetMapping.java
│   ├── PostMapping.java
│   ├── UrlMap.java
│   ├── JsonUrl.java
│   ├── UrlParam.java
│   ├── RequestAttribute.java
│   ├── FormParam.java
│   └── Route.java
├── controller/          # Front Controller
│   └── FrontControllerServlet.java
├── modules/             # Modules principaux
│   ├── ConfigLoader.java
│   ├── ContentRenderManager.java
│   ├── File.java
│   ├── FileHandler.java
│   ├── LogManager.java
│   ├── MethodManager.java
│   ├── ModelView.java
│   ├── RouteMap.java
│   ├── RouteStatus.java
│   └── Url.java
└── utils/               # Utilitaires
    ├── DataTypeUtils.java
    ├── DateUtils.java
    ├── JsonUtils.java
    └── RoutePatternUtils.java
```

---

## 🎯 Exemple d'Utilisation Complet

```java
@Controller(mapping = "/users")
public class UserController {

    @UrlMap("/{id}")
    @GetMapping
    public ModelView show(@UrlParam(name = "id") int userId) {
        ModelView mv = new ModelView();
        mv.setView("user/show.jsp");
        mv.setData("userId", userId);
        return mv;
    }

    @UrlMap("/")
    @GetMapping
    @JsonUrl
    public List<User> list() {
        return userService.findAll();
    }

    @UrlMap("/create")
    @PostMapping
    public String create(User user) {
        userService.save(user);
        return "User created successfully!";
    }

    @UrlMap("/upload")
    @PostMapping
    public String upload(Map<Path, File> files) {
        for (var entry : files.entrySet()) {
            entry.getValue().save();
        }
        return "Files uploaded successfully!";
    }
}
```

---

## 📝 Notes Techniques

- **Java 17+** : Le framework nécessite Java 17 ou supérieur
- **Maven** : Gestion des dépendances via Maven
- **Aucune dépendance externe** : Hormis Jakarta Servlet API (provided)
- **Réflexion** : Utilisation intensive de la réflexion pour l'injection et le binding

---

## 🔐 Sécurité

- Échappement JSON automatique
- Sauvegarde atomique des fichiers
- Validation des types lors de la conversion
- Gestion d'erreurs robuste

---

**Version du framework** : 1.0.0  
**Date de documentation** : 2026-01-14
