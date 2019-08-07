(ns attendify-challange.utils
  (:require [clojure.string :as str]
            [testdouble.cljs.csv :as csv]))

(defn promise[fn] (js/Promise. fn))

(defn file-read-txt [file]
  (promise
   #(let [reader (js/FileReader.)]
      (set! (.-onerror reader) %2)
      (set! (.-onload reader) (fn [x] (%1 (-> x .-target .-result))))
      (.readAsText reader file))))

(defn parse-csv [str]
  (let [data (csv/read-csv str)
        columns (map keyword (first data))]
    [(->> (rest data)
          (map #(zipmap columns %)))
     columns]))

(defn file-read-csv [file]
  (-> (file-read-txt file)
      (.then parse-csv)))

(defn file->map [file]
  (assoc nil
         :name (.-name file)
         :size (.-size file)
         :type (.-type file)
         :last-modified (.-lastModified file)))

(defn str-float? [s] (not (js/isNaN s)))

(defn str-int? [s]
  (and (str-float? s)
       (nil? (str/index-of s \.))
       (nil? (str/index-of s \e))))

(defn ->int [s] (js/parseInt s))

(defn calc-avg [data keys n]
  (->> keys
       (map #(->> data
                  (map (comp ->int %))
                  (reduce +)))
       (map (comp #(.toFixed % n)
                  #(/ % (count data))))
       (zipmap keys)))

(defn- validate-row [r vm]
  (->> (keys vm)
       (map #(if (not (% r))
                 true
                 ((% vm) (% r))))
       (reduce #(and %1 %2))))

(defn validate-data [d vm]
  (->> d
       (map #(validate-row % vm))
       (reduce #(and %1 %2))))
