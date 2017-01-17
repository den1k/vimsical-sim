(ns vimsical.sim.steps.user
  (:require
   [clojure.core.async :as a]
   [clojure.string :as s]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.util.http :as http]))

(def app-user-query
  '[{:app/user
     [:user/uuid
      :user/first-name
      :user/last-name
      :user/email
      {:user/vimsae
       [:vims/uuid
        :vims/title
        {:vims/owner [:user/uuid :user/first-name :user/last-name]}
        {:vims/master-branch
         [:branch/uuid
          {:branch/parent ...}
          :branch/entry-delta-id
          :branch/start-delta-id
          :branch/created-at
          {:branch/files
           [:file/uuid
            :file/type
            :file/sub-type]}
          {:branch/owner
           [:user/uuid
            :user/first-name
            :user/last-name]}]}
        {:vims/branches
         [:branch/uuid
          {:branch/parent ...}
          :branch/entry-delta-id
          :branch/start-delta-id
          :branch/created-at
          {:branch/files
           [:file/uuid
            :file/type
            :file/sub-type]}
          {:branch/owner
           [:user/uuid
            :user/first-name
            :user/last-name]}]}]}
      {:user/settings
       [:settings/uuid
        {:settings/playback
         [:playback/uuid
          :playback/speed]}]}]}])

(defn extract-session-cookie
  [resp]
  {:post [(string? %)]}
  (let [s (get-in resp [:headers "set-cookie"])]
    (first (s/split s #";"))))

(defn merge-ctx
  [ctx resp]
  (assoc ctx :headers {"Cookie" (extract-session-cookie resp)}))

(defn valid-user-resp? [{:keys [body]}] (= body {}))

(defn user-step-fn
  [{:keys [remote-url] :as ctx}]
  {:pre [remote-url]}
  (a/go
    (try
      (debug "Step" ctx)
      (let [req          {:request-method :post
                          :url            remote-url
                          :body           app-user-query}
            resp         (a/<! (http/req-chan req))]
        (when-not (valid-user-resp? resp)
          (error "Invalid resp" resp))
        [(valid-user-resp? resp) (merge-ctx ctx resp)])
      (catch Throwable t
        (error t)))))

(def user-step
  {:name    "Query user"
   :request user-step-fn})
