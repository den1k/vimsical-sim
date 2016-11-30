(ns vimsical.sim.vims
  (:require
   [om.next :as om]
   [vimsical.sim.queries :as q]
   [vimsical.sim.util.http :as http]
   [vimsical.sim.util.ws :as ws]
   [clojure.core.async :as a]))


;; * Step fns

(defn ws-chan-step
  "Assoc the ws-chan in the ctx map"
  [{:keys [ws-url] :as ctx}]
  (a/go
    (let [client-chan (ws/new-client-chan ws-url nil (a/chan) (a/chan))
          conn-chan   (a/<! client-chan)]
      [true (assoc ctx :ws-chan conn-chan)])))

(defn user-step
  "Get the user (query when loading the page)"
  [{:keys [remote-url] :as ctx}]
  {:pre [remote-url]}
  (a/go
    (let [req          {:request-method :post
                        :url            remote-url
                        :body           q/app-user}
          resp         (a/<! (http/req-chan req))
          ring-session (get-in resp [:headers "set-cookie"])]
      [true (assoc-in ctx [:remote-headers "Cookie"] ring-session)])))

(defn new-vims-step
  "Create a new vims"
  [{:keys [remote-url headers] :as ctx}]
  {:pre [remote-url headers]}
  (a/go
    (let [req                     {:headers        headers
                                   :request-method :post
                                   :url            remote-url
                                   :body           (q/vims-new (om/tempid))}
          {:keys [body] :as resp} (a/<! (http/req-chan req))
          app-user-id             (-> body :app/user :db/id)
          vims                    (-> body :user/vimsae last)
          vims-id                 (-> vims :db/id)
          branch                  (-> vims :vims/branches first)
          branch-id               (-> branch :db/id)
          stores                  (-> branch :branch/stores)
          files                   (mapv :store/file stores)]
      [true
       (merge ctx
              {:app-user-id app-user-id
               :vims        vims
               :vims-id     vims-id
               :branch      branch
               :branch-id   branch-id
               :stores      stores
               :files       files})])))

(defn add-change-step
  "Add change to an existing vims"
  [{:as ctx
    :keys
    [ws-chan
     headers
     app-user-id vims-id branch-id
     stores files]}]
  {:pre [ws-chan headers]}
  (let [store-id nil
        file-id  nil
        deltas   nil
        timeline nil]
    (a/go
      (a/>!
       ws-chan
       (q/add-change app-user-id vims-id branch-id file-id store-id deltas timeline)))))
