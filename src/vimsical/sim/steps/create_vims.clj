(ns vimsical.sim.steps.create-vims
  (:require
   [clojure.core.async :as a]
   [om.tempid :as om]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.steps.user :as user]
   [vimsical.sim.util.http :as http]))

;; * Query

(defn- create-vims-query
  [user-id]
  (let [vims-id          (om/tempid)
        branch-id        (om/tempid)
        html-file-id     (om/tempid)
        html-store-id    (om/tempid)
        css-file-id      (om/tempid)
        css-store-id     (om/tempid)
        js-file-id       (om/tempid)
        js-store-id      (om/tempid)
        pointer-file-id  (om/tempid)
        pointer-store-id (om/tempid)]
    (into
     [(list
       'vims/new
       {:user-id user-id,
        :txs
        [{:db/id       user-id,
          :user/vimsae [vims-id]}
         {:db/id              vims-id,
          :vims/owner         user-id,
          :vims/master-branch branch-id,
          :vims/branches      [branch-id]}
         {:db/id             branch-id,
          :branch/start-time 0,
          :branch/author     user-id,
          :branch/stores
          [html-store-id
           css-store-id
           js-store-id
           pointer-store-id]}
         {:db/id      html-store-id,
          :store/file html-file-id}
         {:db/id      js-store-id,
          :store/file js-file-id}
         {:db/id      css-store-id,
          :store/file css-file-id}
         {:db/id      pointer-store-id,
          :store/file pointer-file-id}
         {:db/id             html-file-id,
          :file/content-type "text/html"}
         {:db/id             js-file-id,
          :file/content-type "text/javascript"}
         {:db/id             css-file-id,
          :file/content-type "text/css"}
         {:db/id             pointer-file-id,
          :file/content-type "ui/pointer"}]})]
     user/app-user-query)))

;; * Step

(defn- valid-new-vims-resp? [resp] true)

(defn- merge-vims-ctx
  [ctx {:keys [body] :as resp}]
  {:pre  [body]
   :post [(some? (:app-user-id %))
          (some? (:vims-id %))
          (some? (:branch-id %))
          (seq (:stores %))]}
  (let [{:keys [app/user]}                 body
        {:keys [user/vimsae]}              user
        {:keys [vims/branches] :as vims}   (last vimsae)
        {:keys [branch/stores] :as branch} (first branches)]
    (merge ctx
           {:app-user-id (:db/id user)
            :vims-id     (:db/id vims)
            :branch-id   (:db/id branch)
            :stores      stores})))

(defn create-vims-step-fn
  [{:keys [remote-url headers] :as ctx}]
  {:pre [remote-url headers]}
  (a/go
    (try
      (debug "Step" ctx)
      (let [req  {:headers        headers
                  :request-method :post
                  :url            remote-url
                  :body           (create-vims-query (om/tempid))}
            resp (a/<! (http/req-chan req))]
        (when-not (valid-new-vims-resp? resp)
          (error "invalid resp" resp))
        [(valid-new-vims-resp? resp) (merge-vims-ctx ctx resp)])
      (catch Throwable t
        (error t)))))

(def create-vims-step
  {:name    "Create a new vims AND a new user"
   :request create-vims-step-fn})
