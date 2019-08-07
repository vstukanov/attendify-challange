(ns attendify-challange.core
  (:require [rum.core :as rum]
            [clojure.string :as str]
            [antizer.rum :as ant]
            [attendify-challange.utils :as utils]
            [attendify-challange.components :as componets]))

;;
;; State API
(def ^{:private true} default-app-state {:file nil
                                         :data nil
                                         :columns nil
                                         :error nil})

(defonce app-state (atom default-app-state))

(defn update-record! [index key value]
  (swap! app-state update-in [:data index] assoc key value))

(defn set-file! [file]
  (swap! app-state #(-> %
                        (assoc :file file)
                        (assoc :error nil))))

(defn set-error! [msg]
  (swap! app-state assoc :error msg))

(defn reset-app-state! []
  (reset! app-state default-app-state))

(defn set-data! [data columns]
  (swap! app-state assoc
         :data (vec data)
         :columns columns))

;;
;; Views

(def max-file-size (* 1024 1024))
(def focused-comp (atom nil))

(def ^{:private true} validation-map
  {:Id #(and (utils/str-int? %) ((comp not neg?) (utils/->int %)))
   :SepalLengthCm utils/str-float?
   :SepalWidthCm utils/str-float?
   :PetalLengthCm utils/str-float?
   :PetalWidthCm utils/str-float?
   :Species (comp not empty?)})

(defn- table-column-title [key stats]
  (str (name key)
       (and (key stats)
         (str " [avg = " (key stats) "]"))))

(defn- ->table-column [key stats]
  {:title (table-column-title key stats)
   :dataIndex key
   :key key
   :render (fn [text _ index]
             (let [ef-ref (atom nil)]
               (rum/with-ref
                 (componets/editable-field
                  {:value text
                   :valid? (key validation-map)
                   :on-focus (fn []
                               (if-let [el @focused-comp] (.blur el))
                               (reset! focused-comp @ef-ref))
                   :on-change (fn [v]
                                (reset! focused-comp nil)
                                (update-record! index key v))})
                 #(reset! ef-ref %))))})

(defn on-csv-selected [file]
  (let [file' (utils/file->map file)]
    (if (> (:size file') max-file-size)
      (set-error! "Too large input file.")
      (do
        (set-file! file')
        (-> (utils/file-read-csv file)
            (.then (fn [[data columns]]
                     (set-data! data columns))))))))

(rum/defc app-header < rum/reactive []
  (let [{:keys [file error]} (rum/react app-state)]
    (if (nil? file)
      [:div
       (componets/select-csv-button on-csv-selected)
       (if error
         (ant/alert {:type :error
                     :style {:margin-top 10}
                     :description error
                     :message "Upload Error"}))]
      (componets/file-info file reset-app-state!))))

(rum/defc app-table < rum/reactive []
  (let [{:keys [data columns]} (rum/react app-state)
        stats (utils/calc-avg data [:SepalLengthCm
                                    :SepalWidthCm
                                    :PetalLengthCm
                                    :PetalWidthCm] 3)]
    (if data
      (ant/table {:dataSource data
                  :size :small
                  :bordered true
                  :pagination false
                  :columns (map #(->table-column % stats) columns)}))))

(rum/defc app []
  (ant/layout
   (ant/layout-content
    {:style {:padding 20 :min-height "100vh"}}
    (ant/card [:div
               (app-header)
               (app-table)]))))

(rum/mount (app)
           (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
