# 🔧 BricoliRent

**Application de location d'outils** — Projet académique Jakarta EE

---

## 📋 Description

BricoliRent est une application web de location d'outils professionnels.  
Elle permet à des **clients** de réserver des outils, à des **agents** de gérer les check-outs et retours, et à des **admins** d'administrer le système.

### Fonctionnalités V1
- Réservation d'un seul outil à la fois (avec quantité et période)
- Validation automatique basée sur le score du client
- Check-out et retour gérés par un agent
- Gestion des pénalités de retard
- Traçabilité des paiements et des opérations

---

## 🛠 Technologies

| Composant       | Technologie                     |
|-----------------|----------------------------------|
| Langage         | Java 17                         |
| Build           | Maven (packaging WAR)           |
| Serveur         | WildFly                         |
| Frontend        | JSF / Facelets (XHTML)          |
| CDI             | Jakarta CDI 4.1                 |
| ORM             | Hibernate 6.4 (natif)           |
| Base de données | PostgreSQL                      |
| Architecture    | MVC en couches                  |

---

## 📁 Structure du projet

```
BricoliRent/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/bricolirent/
    │   │   ├── domain/
    │   │   │   ├── entity/      ← Entités Hibernate (User, Client, Tool, etc.)
    │   │   │   └── enums/       ← Enums (ReservationStatus, PaymentType, etc.)
    │   │   ├── repository/      ← Accès données (Hibernate Session)
    │   │   ├── service/         ← Interfaces métier
    │   │   │   └── impl/        ← Implémentations
    │   │   ├── web/
    │   │   │   ├── bean/        ← Managed Beans JSF (CDI)
    │   │   │   └── converter/   ← Convertisseurs JSF
    │   │   ├── security/        ← Sécurité (à venir)
    │   │   ├── config/          ← Configuration (à venir)
    │   │   └── util/            ← HibernateUtil
    │   ├── resources/
    │   │   └── hibernate.cfg.xml
    │   └── webapp/
    │       ├── WEB-INF/         ← web.xml, beans.xml, faces-config.xml
    │       ├── templates/       ← Templates Facelets (layout, header, etc.)
    │       ├── app/             ← Pages applicatives (dashboard, admin, etc.)
    │       ├── resources/css/   ← Feuilles de style
    │       ├── login.xhtml
    │       └── index.xhtml
    └── test/java/
```

---

## 🚀 Lancement du projet

### Prérequis
1. **Java 17** (JDK)
2. **Maven 3.9+**
3. **PostgreSQL** avec une base `bricolirent` créée
4. **WildFly 30+** (ou version compatible Jakarta EE 10)

### Étapes

1. **Créer la base de données PostgreSQL** :
   ```sql
   CREATE DATABASE bricolirent;
   ```

2. **Configurer la connexion** dans `src/main/resources/hibernate.cfg.xml` :
   - URL : `jdbc:postgresql://localhost:5432/bricolirent`
   - Username / Password : adapter selon votre installation

3. **Compiler le projet** :
   ```bash
   mvn clean package
   ```

4. **Déployer le WAR** :
   - Copier `target/bricolirent.war` dans `WILDFLY_HOME/standalone/deployments/`
   - Ou configurer le déploiement dans IntelliJ IDEA

5. **Accéder à l'application** :
   - http://localhost:8080/bricolirent/

---

## 🖥 Intégration IntelliJ IDEA

1. **File → Open** → sélectionner le dossier `BricoliRent`
2. IntelliJ détecte automatiquement le `pom.xml` Maven
3. **File → Project Structure → Project SDK** → choisir Java 17
4. **Run → Edit Configurations → + → JBoss/WildFly Local** :
   - Configurer le chemin vers WildFly
   - Artifact : `bricolirent:war exploded`
   - URL : `http://localhost:8080/bricolirent/`
5. Cliquer sur **Run** pour démarrer

---

## 👥 Acteurs

| Rôle    | Description                                    |
|---------|------------------------------------------------|
| Client  | Réserve des outils, consulte ses réservations  |
| Agent   | Gère les check-outs et retours                 |
| Admin   | Administre les utilisateurs et le catalogue    |

---

## 📄 Licence

Projet académique — Usage éducatif uniquement.
