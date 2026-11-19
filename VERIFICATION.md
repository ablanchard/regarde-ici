# Vérification de la version 0.2 — 19 novembre 2026

## Appareil réel

Galaxy Z Flip6 **SM-F741B**, Android 17 / API 37.

- Activation des deux écrans par **Jetpack WindowManager** depuis l’application, sans commande ADB de changement d’état, root, Shizuku ou Good Lock : réussie.
- Personnage animé visible sur l’écran externe avec aperçu de la caméra arrière sur l’écran principal : confirmé par la capture du panneau externe et par l’utilisateur devant le téléphone.
- Prise de photo pendant une session native active : un nouveau JPEG enregistré dans Pictures/RegardeIci. La photo d’essai reste sur le téléphone ; elle n’est pas publiée.
- Bouton Arrêter : retour au mode normal, état système OPENED (3).
- Retour rapide à l’accueil puis à l’application : processus toujours vivant, aucune nouvelle exception fatale du processus de test.
- Passage à l’accueil pendant deux secondes : retour à OPENED (3) et aucun client caméra actif dans dumpsys media.camera.
- Installation et lancement de l’APK final : réussis, interface et caméra disponibles.

L’ouverture de la présentation et sa fermeture peuvent se croiser lorsqu’on quitte très vite l’application. Le contrôleur invalide les callbacks périmés, ferme les sessions devenues inutiles et intercepte les exceptions de fenêtre si l’écran a disparu.

## Compilation et distribution

- `./gradlew assembleDebug lintDebug` : succès, zéro erreur Lint, treize avertissements non bloquants.
- Vérification apksigner : réussie, signature APK v2.
- Permissions : CAMERA et permission interne AndroidX. Aucune permission Internet, microphone, lecture globale des médias ou contrôle privilégié des états du téléphone.
- Les médias privés, identifiants du téléphone et fichiers de recherche ne sont pas inclus dans les sources ou la release.

## Limites restantes

Cette vérification porte sur un seul téléphone et une seule version du système. Les autres modèles et versions One UI restent à tester. La reprise automatique après rotation est implémentée mais n’a pas été validée physiquement sur ce téléphone. Restent aussi à approfondir : pliage, flash, veille prolongée, batterie faible et persistance des autorisations de vidéo après redémarrage.

La lecture vidéo par VideoView a été vérifiée sur émulateur dans le prototype initial ; la lecture de vidéos personnelles dans une session native sur le Flip6 reste à vérifier. Aucun fichier personnel n’a été importé pour les essais matériels.
