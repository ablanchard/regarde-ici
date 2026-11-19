# Vérification du prototype — 18 novembre 2026

## Vérifié

- `./gradlew assembleDebug lintDebug` : succès, zéro erreur Lint, dix avertissements non bloquants (versions de bibliothèques, extraction de sauvegarde, textes français en code).
- Signature APK : vérification apksigner réussie, schéma v2.
- Permissions APK : CAMERA et permission interne AndroidX ; aucune permission Internet, microphone ou lecture globale de la galerie.
- Installation et lancement de l’APK final sur émulateur Android 11 ARM64.
- Aperçu de la caméra arrière émulée et enregistrement JPEG dans Pictures/RegardeIci.
- Deux photos enregistrées : déclenchement tactile, puis touche de volume avec retardateur de 3 secondes.
- Personnage Canvas visible sur l’écran secondaire simulé pendant l’aperçu photo.
- Import d’une vidéo H.264 MP4 par le sélecteur de documents, lecture sur l’écran secondaire simulé ; fichier de test non inclus dans la livraison.
- Retour au premier plan après le sélecteur de documents.
- Absence de second écran : message explicatif et accès au diagnostic sans plantage.
- Après passage à l’accueil et délai de deux secondes : aucun client caméra actif dans dumpsys media.camera.
- Aucun FATAL EXCEPTION de l’application observé pendant ces essais.

## À vérifier sur un vrai Flip6

Le point central, l’affichage simultané sur l’écran interne et l’écran externe Samsung, n’a pas pu être testé sans téléphone connecté. L’écran simulé est un écran externe Android standard et ne reproduit pas les restrictions Samsung. Cette livraison est un prototype de compatibilité, pas une validation de ce fonctionnement sur le Flip6.

Restent également à vérifier matériellement : mise au point réelle, choix de l’objectif principal, flash, qualité/résolution photo, rotation, pliage, veille, persistance du fichier vidéo après redémarrage et restrictions propres à la version One UI installée.
