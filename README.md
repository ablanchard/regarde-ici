# Regarde ici !

Une application Android pour photographier les enfants avec la caméra arrière du **Galaxy Z Flip6**, pendant qu’un personnage animé ou une vidéo personnelle attire leur regard sur l’écran externe.

## Télécharger et installer

**[Télécharger l’APK v0.2.0](https://github.com/ablanchard/regarde-ici/releases/download/v0.2.0/RegardeIci-prototype.apk)** · [Page de téléchargement](https://github.com/ablanchard/regarde-ici/releases/tag/v0.2.0)

Depuis le téléphone, télécharger l’APK, l’ouvrir avec **Mes fichiers**, puis suivre les instructions d’installation. Si Android le demande, autoriser l’installation depuis cette source. La mise à jour conserve les réglages et les photos.

1. Ouvrir **Regarde ici !** et autoriser la caméra.
2. Garder le téléphone ouvert, objectifs arrière dirigés vers l’enfant.
3. Choisir un personnage ou importer une vidéo locale.
4. Toucher **Activer l’écran externe**, puis cadrer et prendre la photo.

L’activation native a été vérifiée sur un **Galaxy Z Flip6 SM-F741B sous Android 17**. Sur cette configuration, l’application peut activer les deux écrans elle-même : **aucun câble USB, root, Shizuku ou Good Lock n’est nécessaire**.

Le fichier [INSTALLATION.txt](INSTALLATION.txt) contient le guide complet. Si l’écran externe reste inaccessible, ouvrir **Aide → Diagnostic → Copier** dans l’application.

## Fonctions

- Aperçu de la caméra arrière, mise au point tactile, zoom 1× / 2× selon les capacités disponibles.
- Photos JPEG dans **Galerie → RegardeIci**, sans autorisation générale de stockage. La résolution est négociée par CameraX ; le mode Samsung 50 MP n’est pas reproduit.
- Déclenchement tactile ou par une touche de volume, retardateur de 3 secondes, flash désactivé ou automatique.
- Chat, canard et lapin originaux dessinés sur Canvas, avec positions centre / gauche / droite.
- Import d’une vidéo locale via le sélecteur de documents Android, lecture muette en boucle. Si le format est illisible, retour au personnage.
- Bouton **Arrêter**, fermeture de la session d’écran externe et libération de la caméra lorsque l’application quitte le premier plan.
- Diagnostic copiable : modèle, Android, version logicielle, écrans visibles et capacités d’activation native.
- Aucun compte, publicité, accès Internet ou envoi de photos.

## Activation des deux écrans

L’application utilise **Jetpack WindowManager**, via `WindowAreaController.presentContentOnWindowArea`, pour demander au système une session de présentation sur l’écran arrière. Le système allume l’écran correspondant et fournit le conteneur où afficher le personnage ou la vidéo. La caméra et les commandes restent sur l’écran principal.

La session est fermée lorsque l’utilisateur touche **Arrêter** ou lorsque l’application passe en arrière-plan. Une rotation peut recréer l’activité ; la demande d’animation est sauvegardée et rétablie lorsque la nouvelle activité retrouve le premier plan et que le système rend la capacité disponible.

Si cette API n’est pas disponible, l’application peut encore afficher une `Presentation` sur un écran secondaire déjà accessible. Elle ne force aucun état système et n’utilise aucune API privée. Les captures de diagnostic, scripts de recherche et données du téléphone ne font pas partie du dépôt.

Référence Android : [Support foldable display modes](https://developer.android.com/develop/adaptive-apps/guides/foldables/support-foldable-display-modes).

## Compatibilité et limites

La prise en charge de l’activation native dépend des extensions installées par le fabricant. La validation sur le Flip6 sous Android 17 ne garantit pas le même résultat sur un autre modèle ou une autre version One UI. Une session peut être interrompue par le système, notamment au verrouillage, au pliage ou lorsqu’une condition matérielle empêche l’utilisation simultanée des écrans.

L’APK est signé pour le développement. Une distribution en production demanderait une signature dédiée et davantage de tests matériels. Le [rapport de vérification](VERIFICATION.md) précise les essais effectués et les limites restantes.

## Compiler

JDK 17, Android SDK Platform 35 et accès aux dépôts Google/Maven. Configurer `ANDROID_HOME` ou un `local.properties` contenant `sdk.dir=/chemin/vers/Android/sdk`.

```sh
./gradlew assembleDebug lintDebug
```

Résultat : `app/build/outputs/apk/debug/app-debug.apk`.

Versions fixées : Gradle 8.9, Android Gradle Plugin 8.7.3, CameraX 1.5.3, AndroidX Activity 1.10.1 et WindowManager 1.3.0. Android 11 minimum, cible Android 15.
