# 📞 CallScheduler Pro

Application Android de planification d'appels automatiques, optimisée pour Samsung Galaxy S25 Ultra.

---

## 🚀 FONCTIONNALITÉS

### Planification
- ✅ **Appels illimités** : répétition quotidienne, hebdomadaire, jours personnalisés, ou toutes les N jours
- ✅ **Alarmes exactes** (AlarmManager + SCHEDULE_EXACT_ALARM) — précision à la seconde
- ✅ **Persistance après reboot** — les alarmes sont rétablies automatiquement au démarrage
- ✅ **Dates de début/fin** configurables par appel

### Téléphonie
- ✅ **SIM duale** — sélection SIM 1, SIM 2 ou automatique
- ✅ **Postes DTMF** — envoi automatique de tonalités après connexion (pour postes téléphoniques)
- ✅ **Recomposition automatique** si occupé : nombre de tentatives + délai configurables

### Interface (Design System "Deep Space Gold")
- ✅ **Timeline groupée par heure** avec séparateurs dorés
- ✅ **Bannière "Prochain appel"** avec compte à rebours
- ✅ **Tableau de bord** : statistiques en temps réel (total, actifs, passés, taux de réussite)
- ✅ **Historique complet** des 200 derniers appels, groupé par date
- ✅ **Recherche, filtres et tri** multi-critères
- ✅ **Notifications en premier plan** pendant l'exécution des appels
- ✅ **Chips colorés** par statut, groupe, SIM, compteur

---

## 📦 INSTALLATION (Android Studio)

### Prérequis
- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- SDK Android 34 installé
- Appareil physique recommandé (émulateur = pas d'appels réels)

### Étapes

1. **Ouvrir le projet**
   ```
   File → Open → sélectionner le dossier CallScheduler/
   ```

2. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```

3. **Configurer le device**
   - Activer le Mode Développeur sur le S25 Ultra
   - Activer le Débogage USB
   - Connecter via USB

4. **Lancer**
   ```
   Run → Run 'app'
   ```

### Permissions requises (premières ouvertures)
L'app demandera automatiquement :
- **Passer des appels** (CALL_PHONE) — OBLIGATOIRE
- **État du téléphone** (READ_PHONE_STATE) — OBLIGATOIRE
- **Notifications** — OBLIGATOIRE (Android 13+)
- **Alarmes exactes** — OBLIGATOIRE (redirigé vers Paramètres)
- **Optimisation batterie** — Recommandé (désactiver pour la fiabilité)

---

## 🏗️ ARCHITECTURE

```
com.callscheduler/
├── CallSchedulerApp.kt         # Application + canaux notifications
├── MainActivity.kt             # Activité + navigation Compose
│
├── data/
│   ├── model/
│   │   └── ScheduledCall.kt    # Modèles de données (Room @Entity)
│   └── repository/
│       ├── Database.kt         # Room DB + DAOs
│       └── CallSchedulerRepository.kt  # Logique métier + alarmes
│
├── service/
│   ├── AlarmReceiver.kt        # BroadcastReceiver alarmes + boot
│   ├── CallExecutorService.kt  # ForegroundService d'appel
│   └── ScheduleMonitorService.kt  # Service de reschedule
│
└── ui/
    ├── MainViewModel.kt        # ViewModel + états UI
    ├── theme/
    │   └── Theme.kt            # Palette, typographie, couleurs
    └── screens/
        ├── MainScreen.kt       # Écran principal + liste
        ├── AddEditCallScreen.kt  # Formulaire ajout/modification
        └── HistoryScreen.kt    # Historique des appels
```

---

## ⚠️ NOTES IMPORTANTES

### SIM Duale (Samsung)
Le sélecteur SIM utilise des extras propriétaires Samsung (`com.samsung.android.telecom.extra.slot`).
Sur le S25 Ultra, cela devrait fonctionner. Si SIM 2 n'est pas sélectionnée correctement,
aller dans Paramètres → Connexions → Gestionnaire de carte SIM → Appels → Sélectionner SIM manuellement.

### Fiabilité des alarmes (Android 14+)
Samsung One UI peut tuer les services en arrière-plan. Pour une fiabilité maximale :
1. Désactiver l'optimisation batterie pour l'app
2. Autoriser le démarrage automatique
3. Fixer l'app en arrière-plan (maintenir dans le gestionnaire de tâches)

### DTMF
Le délai d'envoi DTMF par défaut est 5 secondes après connexion.
Ajuster si le serveur répond plus lentement.

---

## 📋 MIGRATION DEPUIS L'ANCIENNE APP

Appels visibles dans ta capture d'écran à recréer :

| Nom        | Numéro      | Poste   | Heure | SIM |
|------------|-------------|---------|-------|-----|
| Takfa Début| 0986002140  | 914835  | 19:59 | 1   |
| Razik Fin  | 0986002140  | 739286  | 08:01 | 2   |
| Razik Début| 0986002140  | 739286  | 19:59 | 1   |
| Farid Début| 0986002140  | 973380  | 13:59 | 1   |
| Nacera Fin | 0986002140  | 759891  | 08:01 | 2   |
| Nacera Début| 0986002140 | 759891  | 19:59 | 1   |
| Farid Fin  | 0986002140  | 973380  | 20:0x | 2   |

💡 Suggestion : grouper par nom (Razik, Farid, Nacera, Takfa) via l'étiquette Groupe.
