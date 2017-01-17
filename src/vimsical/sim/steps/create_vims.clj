(ns vimsical.sim.steps.create-vims
  (:require
   [clojure.core.async :as a]
   [om.tempid :as om]
   [taoensso.timbre :refer [debug error]]
   [vimsical.sim.steps.user :as user]
   [vimsical.sim.util.http :as http]
   [vimsical.sim.util.uuid :as uuid]))

;; * Query

(defn- create-vims-query
  []
  (let [user-uuid (uuid/uuid)
        vims-uuid      (uuid/uuid)
        branch-uuid    (uuid/uuid)
        html-file-uuid (uuid/uuid)
        css-file-uuid  (uuid/uuid)
        js-file-uuid   (uuid/uuid)]
    (into
     [(list
       'vims/new
       {:tx
        {:user/uuid user-uuid,
         :user/vimsae
         [{:vims/uuid          vims-uuid
           :vims/title         "foo"
           :vims/owner         {:user/uuid       user-uuid
                                :user/first-name "foo"
                                :user/last-name  "bar"}
           :vims/master-branch {:branch/uuid branch-uuid}
           :vims/branches      [{:branch/uuid  branch-uuid
                                 :branch/owner {:user/uuid user-uuid}
                                 :branch/files [{:file/uuid     html-file-uuid
                                                 :file/type     :text
                                                 :file/sub-type :html}
                                                {:file/uuid     css-file-uuid
                                                 :file/type     :text
                                                 :file/sub-type :css}
                                                {:file/uuid     js-file-uuid
                                                 :file/type     :text
                                                 :file/sub-type :javascript}]}]}]}})]
     user/app-user-query)))

;; * Step

(defn- valid-new-vims-resp? [resp] true)

(defn- merge-vims-ctx
  [ctx {:keys [body] :as resp}]
  {:pre  [body]
   :post [(some? (:app-user-uuid %))
          (some? (:vims-uuid %))
          (some? (:branch-uuid %))
          (seq (:files %))]}
  (let [user                              (-> body :app/user :user)
        {:keys [user/vimsae]}             user
        {:keys [vims/branches] :as vims}  (last vimsae)
        {:keys [branch/files] :as branch} (first branches)]
    (merge ctx
           {:app-user-uuid (:user/uuid user)
            :vims-uuid     (:vims/uuid vims)
            :branch-uuid   (:branch/uuid branch)
            :files         files})))

(defn create-vims-step-fn
  [{:keys [remote-url headers] :as ctx}]
  {:pre [remote-url headers]}
  (a/go
    (try
      (debug "Step" ctx)
      (let [req  {:headers        headers
                  :request-method :post
                  :url            remote-url
                  :body           (create-vims-query)}
            resp (a/<! (http/req-chan req))]
        (when-not (valid-new-vims-resp? resp)
          (error "invalid resp" resp))
        [(valid-new-vims-resp? resp)
         (merge-vims-ctx ctx resp)])
      (catch Throwable t
        (error t)))))

(def create-vims-step
  {:name    "Create a new vims AND a new user"
   :request create-vims-step-fn})
