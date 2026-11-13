# Gaganiomètre

Un goniomètre pour Android : il mesure l'amplitude d'une articulation avec les capteurs du téléphone.

Gratuit. Pas d'abonnement, pas de compte, pas de publicité, pas de connexion internet. L'app ne demande aucune permission et les mesures restent sur le téléphone.

Développé avec amour pour Gaga.

## Installer

Téléchargez [`apk/goniometre-debug.apk`](apk/goniometre-debug.apk) sur le téléphone et ouvrez-le. Android demandera d'autoriser l'installation depuis une source inconnue, puisque l'app ne vient pas du Play Store.

Il faut Android 8 ou plus récent.

## Mesurer

1. Posez ou fixez le téléphone sur le segment qui bouge (l'avant-bras pour le coude, par exemple).
2. Placez l'articulation en position de départ et touchez « Mettre à zéro et démarrer ».
3. Faites le mouvement. L'angle s'affiche en direct et le maximum atteint est conservé.
4. Touchez « Terminer ». Vous pouvez ajouter une note et enregistrer la mesure.

Deux façons de démarrer :

- **Mesure rapide** : le bouton orange en haut. N'importe quel angle, sans valeur de référence.
- **Par articulation** : épaule, coude, poignet, hanche, genou, cheville, rachis cervical et thoraco-lombaire. Le résultat est comparé à l'amplitude normale du mouvement choisi.

L'historique garde les mesures enregistrées et permet de les partager en texte.

## Capteurs

- **Gyroscope** (par défaut) : fonctionne dans tous les plans, quelle que soit la façon dont le téléphone est posé.
- **Accéléromètre** : pour les téléphones sans gyroscope. Le mouvement doit incliner le téléphone par rapport à la verticale, et il faut bouger lentement.

## À savoir

Ce n'est pas un dispositif médical. Les amplitudes normales affichées sont des valeurs de référence usuelles, données à titre indicatif, et la précision dépend des capteurs du téléphone et de la façon dont il est tenu.

L'interface est en français ou en anglais, selon la langue du téléphone.

## Compiler

Avec le SDK Android et un JDK 17 ou plus récent :

```
./gradlew assembleDebug
```

L'APK est généré dans `app/build/outputs/apk/debug/`.

## Licence

GPL v3, voir [LICENSE](LICENSE).

---

## English

A goniometer for Android: it measures a joint's range of motion with the phone's sensors.

Free. No subscription, no account, no ads, no internet connection. The app asks for no permissions and measurements stay on the phone.

Download [`apk/goniometre-debug.apk`](apk/goniometre-debug.apk) on the phone and open it (Android 8 or later). Put the phone on the moving segment, zero at the starting position, do the movement, and read the maximum angle. Use the orange button for a quick measurement of any angle, or pick a joint to compare the result with the usual normal range.

This is not a medical device; the normal ranges are indicative reference values.
