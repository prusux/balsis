# Balsis 🎙️

**Balsis** ir Android lietotne, kas ļauj transkribēt un apkopot WhatsApp balss ziņas, nepārtraucot mūzikas, podkāstu vai video atskaņošanu.

Lietotne izmanto Google Gemini Flash modeli ar īpaši pielāgotu latviešu valodas uzvedni, sniedzot gan ātru 1-2 teikumu kopsavilkumu (TL;DR), gan pilnu vārdisko transkripciju, kā arī saglabā ziņas vietējā SQLite/Room datubāzē ar meklēšanas iespēju.

---

## Galvenās Iespējas

- ⚡ **Nepārtrauc mūziku**: Atveras kā caurspīdīgs peldošais logs (*bottom sheet*) tieši virs WhatsApp čata, nepauzējot audio fonā.
- 🎯 **Būtība (TL;DR)**: Izceļ galveno informāciju (laiku, vietu, jautājumu, vienošanos), izlaižot tukšu runāšanu un pauzes.
- 🇱🇻 **Teicama latviešu valoda**: Atpazīst latviešu sarunvalodu, žargonu un saglabā visas garumzīmes un mīkstinājuma zīmes.
- 👤 **Automātiska sūtītāja noteikšana**: Izmantojot Android paziņojumu klausītāju, automātiski piesaista balss ziņai sūtītāja un čata vārdu.
- 📚 **Meklējama vēsture**: Visas transkripcijas tiek droši saglabātas telefonā, lai jebkurā brīdī varētu pārlasīt iepriekš saņemtās ziņas.
- 🔐 **BYOK (Bring Your Own Key)**: Jūsu audio dati nepieder nevienam trešajam pakalpojumam. API atslēga tiek glabāta šifrēti Jūsu ierīcē (*Android Keystore*).

---

## Kā Iegūt Bezmaksas Google AI Studio API Atslēgu

1. Dodieties uz [Google AI Studio (aistudio.google.com)](https://aistudio.google.com/).
2. Piesakieties ar savu Google kontu.
3. Noklikšķiniet uz **"Get API key"** -> **"Create API key"**.
4. Nokopējiet iegūto atslēgu (sākas ar `AIzaSy...`).
5. Atveriet lietotni **Balsis** -> Iestatījumi -> Ielīmējiet atslēgu un nospiediet **"Saglabāt"**.

*Piezīme: Google AI Studio bezmaksas līmenis sniedz līdz 15 pieprasījumiem minūtē, kas ir vairāk nekā pietiekami ikdienas lietošanai bez maksas.*

---

## Kā Lietot

1. WhatsApp čatā nospiediet un turiet balss ziņu (vai vairākas).
2. Spiediet **Kopīgot (Share)** pogu.
3. Izvēlieties **Balsis**.
4. Ekrāna apakšā uzreiz parādīsies kopsavilkums un pilns teksts!
5. Lai pārskatītu vecas ziņas, atveriet lietotni **Balsis** no sākuma ekrāna.

---

## Projekta Būvēšana (Build from Source)

Prasības:
- JDK 17 vai jaunāks (piem. Android Studio JBR)
- Android SDK 35 (Android 14/15)

```bash
git clone https://github.com/prusux/balsis.git
cd balsis
./gradlew assembleDebug
```
Gatavais APK fails tiks izveidots mapē: `app/build/outputs/apk/debug/app-debug.apk`.

---

## Licence

MIT License.
