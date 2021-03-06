(ns brainiac.plugins.twitter-search
  (:import [java.text SimpleDateFormat]
           [java.util TimeZone])
  (:use [clojure.contrib.json :only (read-json)]
        [clojure.java.io :only (reader)])
  (:require [brainiac.plugin :as brainiac]))

(def time-format
  (let [date-format (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss ZZZZZ")]
    (.setTimeZone date-format (TimeZone/getTimeZone "America/Chicago"))
    date-format))

(defn now [] (System/currentTimeMillis))

; shamelessly adapted from Rails' DateHelper
; https://github.com/rails/rails/blob/fbf23ed9338f935736dd840ea973fd6372b6ab7d/actionpack/lib/action_view/helpers/date_helper.rb#L67
(defn time->time-ago-words [time]
  (let [delta (quot (- (now) (.getTime (.parse time-format time))) 60000)]
    (cond
      (= delta 0) "less than a minute ago"
      (= delta 1) "a minute ago"
      (<= delta 44) (str delta " minutes ago")
      (<= delta 89) (str "about 1 hour ago")
      (<= delta 1439) (str (quot delta 60) " hours ago")
      (<= delta 2519) "1 day ago"
      (<= delta 43199) (str (quot delta 1440) " days ago")
      (<= delta 86399) "about 1 month ago"
      (<= delta 525599) (str (quot delta 43200) " months ago")
      (<= delta 1051199) "a year ago"
      :else (str (quot delta 525600) " years ago")
      )))

(defn format-tweet [tweet]
  {
   :name (:from_user_name tweet)
   :handle (:from_user tweet)
   :time (time->time-ago-words (:created_at tweet))
   :text (:text tweet)
   :profile_image_url (:profile_image_url_https tweet)
   })

(defn transform [stream]
  (let [json (read-json (reader stream))]
  (assoc {}
    :name "twitter-search"
    :data (map format-tweet (:results json)))))

(defn url [term]
  (format "http://search.twitter.com/search.json?q=%s" term))

(defn configure [{:keys [term program-name]}]
  (brainiac/simple-http-plugin
    {:url (url term)}
    transform program-name))
