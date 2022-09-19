(ns squarity.app.chess)


(defn light?
  [i]
  ; The fact that the width/height of a chess board, 8,
  ; is a power of the binary base, lets us take advantage of bit
  ; arithmetics. The 0'th bit toggles when x is incremented,
  ; while the 3 bit toglles with y is incremented. This is when
  ; indexing is done from top-left to bottom-right.
  (= (bit-test i 0) (bit-test i 3)))

(defn color-of
  [i]
  (if (light? i)
    :light
    :dark))

(defn x
  [i]
  (mod i 8))

(defn y
  [i]
  (quot i 8))

(def x-names ["a" "b" "c" "d" "e" "f" "g" "h"])

(def y-names ["8" "7" "6" "5" "4" "3" "2" "1"])

(defn name-of 
  [square]
  (when square
    (str (x-names (x square)) (y-names (y square)))))
