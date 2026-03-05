# Szakdolgozat
Szoftver Felhasználói Kézikönyv és Rendszerleírás

Interaktív Többállomásos gyártósor ütemező rendszer

1. Bevezetés és Rendszeráttekintés
A kifejlesztett alkalmazás egy modern, Ipar 4.0 irányelvekre épülő, Java nyelven írt asztali szoftver, amely a Gyártásvégrehajtási Rendszerek (MES - Manufacturing Execution System) és a Fejlett Tervező és Ütemező Rendszerek (APS - Advanced Planning and Scheduling) funkcióit ötvözi.
A szoftver célja egy klasszikus Flow Shop gyártási környezet modellezése, szimulációja és heurisztikus optimalizálása. Képes kezelni a valós ipari környezetben felmerülő legkritikusabb peremfeltételeket: a sorrendfüggő átállási időket, a gépek váratlan meghibásodását (karbantartás), a megrendelések beérkezési idejét, a szigorú határidőket, valamint a kiemelt (VIP) ügyfelek sürgős rendeléseit.
A rendszer öt fő logikai modulra oszlik, amelyek a gyártástervezés teljes vertikumát lefedik az adatbeviteltől a vizuális ütemezésen át a fejlett adatelemzésig.
________________________________________
2. A Szoftver Elméleti és Matematikai Háttere
Mielőtt a konkrét funkciók bemutatására térnénk, elengedhetetlen a szoftver "motorját" adó matematikai modell ismertetése. A program minden kiértékelést egy összetett célfüggvény alapján végez.
2.1. Célfüggvény és Büntetőpont-számítás
A rendszer a beépített "Lokális Kereső" algoritmus futtatása során azt a gyártási sorrendet keresi, amely minimalizálja az összesített büntetőpontot. A pontszám a következőkből tevődik össze:
	C_max (Átfutási idő): Az az időpont, amikor a legutolsó termék is legördül a legutolsó gépről. A cél ennek minimalizálása a gépek üresjáratainak csökkentése érdekében.
	Súlyozott Késés: Minden termék rendelkezik egy határidővel (DueDate). Ha a termék befejezési ideje meghaladja a határidőt, a program kiszámolja a késés mértékét, és megszorozza a termék fontosságával (Weight).
	Sürgős (VIP) Rendelések kezelése: Amennyiben egy megrendelés "VIP" jelölést kap, a szoftver a késéséből fakadó büntetőpontot megszorozza $1000$-rel. Ez az algoritmust arra kényszeríti, hogy a VIP termékeket minden esetben a gyártási sor elejére (vagy biztonságos pozícióba) mozgassa, elkerülve a gigantikus kötbéreket.

2.2. Költségkalkulációs (Pénzügyi) Modell
A szoftver nemcsak absztrakt pontszámokkal, hanem forintosított értékekkel is dolgozik, megmutatva a mérnöki optimalizálás gazdasági hasznát.
	Gépüzemeltetési költség: C_MAx  x gépek száma x 1500Ft.
	Késedelmi kötbér: ∑ (Késés x Súly x 5000Ft).
________________________________________
3. Vezérlőpult Modul
A rendszer indulásakor a Vezérlőpult fogadja a felhasználót. Ez a felület szolgál a szimuláció irányítására, valamint a végeredmény azonnali vizuális és pénzügyi kiértékelésére.
3.1. Alap Paraméterek és Algoritmus vezérlő
A képernyő felső sávjában állíthatók be a szimuláció alapparaméterei:
	Munkák (NJ) és Gépek (NR): Meghatározza a probléma méretét (Termékek és Erőforrások száma).
	Algoritmus lenyíló menü: Két opciót kínál. A „Kezdeti (Ad-hoc)” beállítás optimalizálás nélkül, a beérkezés nyers sorrendjében szimulálja le a gyártást. A „Lokális Kereső” beállítás aktiválja a mesterséges intelligenciát (heurisztikát), amely megpróbálja megtalálni az optimális sorrendet.
	Lépések (STEP) és Szomszédok (LOOP): Az optimalizáló algoritmus iterációinak számát szabályozza. Minél magasabb az érték, annál nagyobb a keresési tér, de annál több számítási kapacitást igényel.
3.2. Fő Műveleti Gombok
	Új Adatok: Újragenerálja a teljes rendszert (termékek, műveleti idők, átállások) a beállított NJ és NR értékek alapján véletlenszerű paraméterekkel.
	Import / Export (TXT és CSV): Lehetőséget biztosít az aktuális gyártási környezet kimentésére, illetve betöltésére. Különösen fontos az ipari CSV export/import funkció, amely lehetővé teszi a vállalat meglévő (pl. Excel alapú) adatbázisaiból a valós rendelésállományok betöltését a programba.
	Szimuláció Indítása: Kék színnel kiemelt főgomb. Ennek megnyomására indul el a számítási folyamat a háttérben.





3.3. Műveleti Idők Mátrixa (ProcT)
Egy "Read-Only" táblázat, amely összefoglalja az aktuális rendelésállományt. Megmutatja az egyes termékek (Job-ok) ID-jét, márkanevét (a VIP státuszt ⭐ ikon jelöli), és azt, hogy az adott termék megmunkálása az egyes gépeken hány időegységet vesz igénybe. A táblázat cellái animáció során kiszíneződnek, mutatva az éppen gyártás alatt álló termékeket.
3.4. Vizuális Gantt Diagram
A szoftver központi vizualizációs eszköze, amely az ütemtervet ábrázolja grafikus formában. Részletes jelmagyarázata a következő:
	Színes téglalapok: Magát a hasznos gépi megmunkálást (Processing) jelölik. Minden termék saját, egyedi színkóddal rendelkezik a könnyű követhetőség érdekében.
	Szürke téglalapok a színesek előtt: A sorrendfüggő átállási időket (Setup Time) jelölik. Az algoritmus egyik fő feladata ezen szürke területek minimalizálása.
	Piros, áttetsző blokkok: A rögzített gépleállásokat, tervezett karbantartásokat szimbolizálják.
	Okos Határidő Zászlók (Smart Flags): A legutolsó gép (kiszállítás) soránál minden termék blokkjához tartozik egy színazonos zászlócska, belsejében a termék azonosítójával (pl. J3). Ez a zászló mutatja a termék ügyfél által kért határidejét.
	Késés vizualizáció: Amennyiben egy termék blokkja túllóg a saját zászlóján (késik), a blokk kerete vastag pirosra, a zászlót bekötő vonal pedig piros szaggatott vonalra vált, azonnali vizuális riasztást adva a tervezőnek.
A Gantt diagram alatt található egy lejátszó csúszka, amellyel a gyártási folyamat percről percre, animálva is visszanézhető.
3.5. Kulcsmutatók (KPI Kártyák)
A képernyő alján négy nagyméretű, színes kártya mutatja a szimuláció eredményét:
	Kezdeti Büntetőpont: Az ad-hoc (optimalizálatlan) sorrendből fakadó pontszám.
	Optimalizált Eredmény: Az algoritmus által talált legjobb sorrend pontszáma.
	Javulás (%): A két állapot közötti százalékos fejlődés.
	Megtakarított Költség: A beépített pénzügyi modell alapján kiszámolt virtuális megtakarítás, amely az elkerült kötbérekből és a rövidült gépidőből adódik.
________________________________________
 
4. Termékek és Karbantartás Modul
Ez a felület felelős az adatbázis manuális módosításáért (CRUD műveletek: Create, Read, Update, Delete). A szoftver beépített biztonsági mechanizmussal rendelkezik: bármilyen adat hozzáadása, módosítása vagy törlése előtt a rendszer az admin jelszót kéri, megelőzve az illetéktelen beavatkozást az ütemtervbe.
4.1. Gyártandó Termékek Kezelése
A felső táblázat listázza a rendszerben lévő megrendeléseket. Az alatta lévő gombokkal (Új, Módosítás, Törlés) a következő paraméterek paraméterezhetők interaktív űrlapokon keresztül:
	Név (Márka): A termék azonosítására szolgál (legördülő listából választható).
	Sürgős (VIP) Checkbox: Kipipálásával a termék kritikus prioritást kap a rendszerben.
	Súly (Weight): 1-től 10-ig terjedő skálán a termék üzleti fontosságát jelöli.
	Határidő (Due Date): A kiszállítás várható ideje.
	(Megjegyzés: Új termék hozzáadásakor a szoftver automatikusan megnöveli a dinamikus átállási mátrixot, és generálja a szükséges kapcsolódó időket).
4.2. Aktív Karbantartások (Leállások) Kezelése
Az ipari gyártásban a gépek sosem futnak 100%-os rendelkezésre állással. Az alsó táblázatban a tervező manuálisan rögzíthet "kieső időablakokat".
	Paraméterek: Megadható a Gép sorszáma, a leállás kezdete (T1) és vége (T2).
	Működési logika: Ha az algoritmus azt észleli, hogy egy termék megmunkálása ütközne a karbantartással, automatikusan megszakítja a folyamatot, kivárja a karbantartás végét, és csak utána engedi a gépet újraindulni. Ezek a leállások piros blokként jelennek meg a Gantt-diagramon.
________________________________________
 
5. Gépkihasználtság (OEE) Modul
A TPM (Total Productive Maintenance) és a Lean gyártás alapvető metrikája az OEE (Overall Equipment Effectiveness). A harmadik fül ezt vizualizálja.
5.1. Százalékos sávdiagramok
A program minden egyes gépre kiszámolja és felrajzolja a teljes üzemidő megoszlását:
	Zöld sáv (Hasznos Gyártás): A termék tényleges, értékteremtő megmunkálásával töltött idő. A sáv végén százalékosan is megjelenik az érték.
	Szürke sáv (Átállási Idő): Veszteségidő, amelyet a gépek felkészítése (Setup) emészt fel.
	Világosszürke sáv (Üresjárat): Az az idő, amikor a gép áll, mert vagy karbantartás alatt van, vagy alapanyagra vár az előző géptől (starvation jelenség).
5.2. Szűk Keresztmetszet (Bottleneck) Detektálás
A szoftver a háttérben folyamatosan figyeli a gépek terheltségét. A képernyő legalsó sorában, piros kiemeléssel automatikusan megjeleníti a "Szűk Keresztmetszetet". Ez az a gép, amelynek a legalacsonyabb az üresjárati ideje (legmagasabb a kombinált hasznos és átállási ideje). Ez az információ kritikus a menedzsment számára a kapacitásbővítési döntések meghozatalakor.
________________________________________
 
6. Trendek és Történet
Egy professzionális APS rendszer nemcsak a jelent, hanem a múltbéli futtatások eredményeit is képes elemezni. A negyedik fül egy beépített adatvizualizációs központ (BI Dashboard).
6.1. Adatnaplózás és Táblázat
A szoftver minden "Szimuláció Indítása" gombnyomást egyedi "Futás ID"-val lát el, és rögzíti annak paramétereit: használt algoritmus, Kezdeti és Optimalizált pontszám, C_MAx  százalékos javulás, valamint a megtakarított összeg. Ezek az adatok egy részletes naplótáblázatban jelennek meg.
6.2. Dinamikus Okos-Grafikon
A táblázat felett egy egyedi fejlesztésű, görgethető X-Y vonaldiagram található, amely vizuálisan ábrázolja a rendszer teljesítményének alakulását a futtatások során.
	Interaktív Szűrők: A grafikon feletti "Megjelenített metrikák" szekcióban checkboxok (Kezdeti, Opt., Cmax, Javulás, Költség) segítségével választható ki, hogy mik kerüljenek a diagramra. Az "Összes" gombbal egy kattintással be- és kikapcsolható minden vonal.
	Skálatorzítás Elleni Védelem (Relatív Y-tengely): Amennyiben a felhasználó olyan adatokat jelöl ki egyszerre, amelyek nagyságrendje eltérő (például egy 85\%-os javulás és egy 12 000 000Ft-os költség), a grafikon intelligensen átvált egy 0% - 100% közötti relatív skálára. Ilyenkor minden adatsor a saját maximumához (amely a tapadós jelmagyarázatban olvasható) képest rajzolódik ki, így a görbék tökéletesen összehasonlíthatók maradnak az egyetlen képernyőn.
	Adatsorok némítása: A táblázat legelső oszlopában lévő pipák kiszedésével az irreleváns vagy extrém kiugró futások egy kattintással eltüntethetők a grafikonról. A fejlécben található fő-pipával az összes sor egyszerre kezelhető.
	A grafikon reszponzív: ha túl sok futás gyűlik össze, a pontok nem nyomódnak össze, hanem a panel kiszélesedik, és egy vízszintes görgetősáv jelenik meg a kényelmes navzaigációhoz. A jelmagyarázat görgetés közben is mindig a képernyő bal oldalán marad (tapadós UI).
6.3. Történet Export/Import
A felül található gombokkal a teljes futási napló kimenthető CSV formátumba későbbi Exceles elemzéshez, vagy egy korábbi munkamenet maradéktalanul visszatölthető a szoftverbe (például egy vezetői prezentációhoz).
________________________________________
 
7. Részletes Eredmények (Log) Modul
Az utolsó fül egy hagyományos, "konzol" stílusú kimenetet biztosít, amely transzparensé teszi az algoritmus működését.
	ASCII Gantt Diagram: Egy karakteres alapú idővonal-megjelenítés, amely gyors áttekintést nyújt a gépek foglaltságáról olyan rendszereken is, ahol a grafikus megjelenítés korlátozott.
	Késések Részletezése: A log legfontosabb része. Lista formájában lebontja a legutolsó gépről (kiszállításról) lekerülő termékeket. Soronként kiírja a termék azonosítóját, befejezési idejét, a hozzárendelt határidőt, az ebből fakadó késést, valamint a termék súlyát. A VIP termékeket külön [VIP] címkével jelöli a listában, egyértelművé téve, hogy a heurisztikus algoritmus miért hozta meg az adott ütemezési döntést.
8. Összegzés és Alkalmazhatóság
A bemutatott szoftver sikeresen bizonyítja, hogy az ipari gyártástervezés klasszikus problémái (késések, gépleállások, magas átállási idők) hatékonyan kezelhetők egyedi szoftveres megoldásokkal. Az interaktív vizualizáció, a beépített költségkalkulációs logika, valamint a mélyreható Business Intelligence (trend) modul alkalmassá teszi az alkalmazást mind oktatási/demonstrációs célokra, mind pedig a modern, adatvezérelt Ipar 4.0-ás gyárak napi szintű termelésütemezésének támogatására.

