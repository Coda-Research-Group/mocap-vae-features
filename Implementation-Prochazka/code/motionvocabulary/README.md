# Creating a motion vocabulary

Cesty nize jsou relativne vuci ~xdohnal/research/motion-words

```
export CLASSPATH="MESSIF.jar:MESSIF-Utility.jar:MotionVocabulary.jar:commons-cli-1.4.jar:smf-core-1.0.jar:smf-impl-1.0.jar:MCDR.jar:m-index.jar:trove4j-3.0.3.jar"

java -Xmx${MEM:-500m} -cp $CLASSPATH messif.motionvocabulary.MotionVocabulary -d $DATAFILE -c $CLS_OBJ --quantize $TOSEQ ${VOCTYPE} $VOCABULARY $SOFTASSIGNPARAM --output $OUTPUT
```

Vyse uvedeny prikaz pro spusteni implementace vyzaduje nasledujici promenne jako parametry, ktere urcuji jaka data se maji zpracovavat a jake motion words produkuje (hard vs. soft):

```
CLS_OBJ=mcdr.sequence.impl.SequenceMocapPoseCoordsL2DTW
DATAFILE=hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12.data
SOFTASSIGNPARAM="--soft-assign D80K6"   # optional (default is D0K1)
TOSEQ="--tosequence"   # set if you need to convert the input file of segments to motion words _and_ merge the segments back to sequences/actions
OUTPUT="...." # output file with the input data quantized to motion words
```

## Hard/soft vocabulary pomoci Voronoi diagramu

**Recommended**

Predem dane pivoti jsou nacteny a vytvoren Voronoi diagram:

```
VOCTYPE="-v"
VOCABULARY="hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12/pivots-kmedoids-350.data"
```

## Hard/soft vocabulary pomoci M-indexu

**For experts only**

Jde o opakovane vyuziti jedne sady pivotu k jemnejsimu rozdeleni prostoru pomoci *pseudo-rekurzivniho* Voronoi diagramu, presneji, pivot-permutation prefixu:

```
VOCTYPE="--mindex"
VOCABULARY="hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12/kmedoids/kmedoids-segment80_shift16-coords_normPOS-fps12-pivots350-maxlvl1-leaf999999.bin"
```

V adresari je spousta dalsich pivots*.data* souboru. Doporucuji ty kmedoids nebo hkmedoid.

Pro jine slovniky kouknete do ostatnich adresaru hdm05-annotations_specific-segment*_shift*-coords_normPOS-fps12. 
Mely byt tam byt min. ty pivots*data* soubory.


## Multioverlay vocabulary

```
VOCTYPE=--mmindex
binarky jsou pak v podadresari kmedoids-multioverlay, napr.
VOCABULARY=hdm05-annotations_specific-segment80_shift16-coords_normPOS-fps12/kmedoids-multioverlay/mmindex-overlays5-segment80_shift16-coords_normPOS-fps12-pivots350-maxlvl1-leaf999999.bin
```
