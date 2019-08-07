(ns attendify-challange.components
  (:require [rum.core :as rum]
            [antizer.rum :as ant]))

(rum/defc select-csv-button [on-select]
  (ant/upload {:accept ".csv"
               :multiple false
               :show-upload-list false
               :before-upload (fn [] false) ; we have to discard uploading
               :on-change (fn [result] (on-select (-> result .-file)))}
              (ant/button [(ant/icon {:type :file}) "Select CSV file"])))

(rum/defcs editable-field <
  ;; Local State
  (rum/local false ::edit?)

  ;; Class Methods
  {:class-properties
   {:blur #(this-as comp
             (-> @(rum/state comp)
                 (::edit?)
                 (reset! false)))}}

  ;; Render
  [state {:keys [value on-focus on-change valid?]}]
  (let [edit? (::edit? state)
        e->val #(-> % .-target .-value)]
    (if @edit?
      (ant/input {:default-value value
                  :autoFocus true
                  :on-press-enter (fn [e]
                                    (let [new-value (e->val e)
                                          valid? (or valid? identity)]
                                      (if (valid? new-value)
                                        (do
                                          (reset! edit? false)
                                          (on-change new-value))
                                        (do
                                          (js/alert "Invalid value")))))})
      [:div {:style {:cursor "pointer"
                     :height 32
                     :line-height "32px"
                     :padding-left 12}
             :on-click (fn []
                         (reset! edit? true)
                         (on-focus))}
       value])))

(defn- file-info-item [title value]
  [:div {:style {:margin-right 8}}
   [:dt title]
   [:dd (ant/tag value)]])

(rum/defc file-info [file on-close]
  (let [{:keys [name size type]} file]
    [:div
     [:h2 name " " (ant/button {:shape :circle
                                :icon :close
                                :on-click on-close})]
     [:dl {:style {:display "flex"}}
      (file-info-item "Size" size)
      (file-info-item "Type" type)]]))
