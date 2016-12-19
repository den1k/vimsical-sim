(ns vimsical.sim.steps.user
  (:require
   [clojure.core.async :as a]
   [clojure.string :as s]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.util.http :as http]))

(def app-user-query
  '[{:app/user
     [:db/id
      :user/first-name
      :user/last-name
      :user/email
      :user/password
      {:user/vimsae
       [:db/id
        :vims/title
        {:vims/owner [:db/id :user/first-name :user/last-name]}
        {:vims/master-branch
         [:db/id
          :branch/start-time
          :branch/end-time
          {:branch/author [:db/id :user/first-name :user/last-name]}
          {:branch/stores
           [:db/id
            {:store/file [:db/id :file/content-type]}
            :store/deltas
            :store/timeline
            :store.derived/current-delta-id]}]}
        {:vims/branches
         [:db/id
          :branch/start-time
          :branch/end-time
          {:branch/author [:db/id :user/first-name :user/last-name]}
          {:branch/stores
           [:db/id
            {:store/file [:db/id :file/content-type]}
            :store/deltas
            :store/timeline
            :store.derived/current-delta-id]}]}
        {:vims.derived/slices
         [:slice/start
          :slice/end
          :slice/duration
          :slice/branch-depth
          :slice/time-in-branch
          :slice/branch]}]}
      {:user/settings
       [:db/id {:settings/playback [:db/id :playback/speed]}]}]}])

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
