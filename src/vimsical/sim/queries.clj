(ns vimsical.sim.queries
  (:require
   [om.next :as om]))

(def app-user
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

(defn vims-new
  [user-id]
  `[(vims/new
     {:user-id ~user-id,
      :txs
      [{:db/id ~user-id,
        :user/vimsae
        [~(om/tempid "f65f8682-a0e3-42e3-aecd-a26839bd336e")]}
       {:db/id ~(om/tempid "c37707b9-4d7f-426a-bdda-9207a739d84f"),
        :store/file ~(om/tempid "f87533f4-7785-4a64-b6ca-2c71a198939b")}
       {:db/id ~(om/tempid "f87533f4-7785-4a64-b6ca-2c71a198939b"),
        :file/content-type "text/html"}
       {:db/id ~(om/tempid "98a8f950-6999-4477-a3b8-07be31e57f45"),
        :file/content-type "text/javascript"}
       {:db/id ~(om/tempid "f65f8682-a0e3-42e3-aecd-a26839bd336e"),
        :vims/owner ~user-id,
        :vims/master-branch
        ~(om/tempid "e6b0dbe0-a0f3-4c24-b18c-e3ffb56d98e3"),
        :vims/branches [~(om/tempid "e6b0dbe0-a0f3-4c24-b18c-e3ffb56d98e3")]}
       {:db/id ~(om/tempid "e6b0dbe0-a0f3-4c24-b18c-e3ffb56d98e3"),
        :branch/start-time 0,
        :branch/author ~user-id,
        :branch/stores
        [~(om/tempid "c37707b9-4d7f-426a-bdda-9207a739d84f")
         ~(om/tempid "5c527a7f-5833-4e83-8462-08ef9d6f69a8")
         ~(om/tempid "e2fffed5-c373-40e0-b821-e0c2d63b2407")
         ~(om/tempid "0e0e443f-e1a5-4b17-9140-97d63d34ea7c")]}
       {:db/id ~(om/tempid "e2fffed5-c373-40e0-b821-e0c2d63b2407"),
        :store/file ~(om/tempid "98a8f950-6999-4477-a3b8-07be31e57f45")}
       {:db/id ~(om/tempid "5c527a7f-5833-4e83-8462-08ef9d6f69a8"),
        :store/file ~(om/tempid "d93864a6-7134-4e41-9d66-4debc141a022")}
       {:db/id ~(om/tempid "0e0e443f-e1a5-4b17-9140-97d63d34ea7c"),
        :store/file ~(om/tempid "bf095dda-83e3-4a97-a03e-5c4e9a76c065")}
       {:db/id ~(om/tempid "d93864a6-7134-4e41-9d66-4debc141a022"),
        :file/content-type "text/css"}
       {:db/id ~(om/tempid "bf095dda-83e3-4a97-a03e-5c4e9a76c065"),
        :file/content-type "ui/pointer"}]})
    {:app/user
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

;; [(vims/add-change
;;   {{:user_id 17592186045568,
;;     :vims_id 17592186045603,
;;     :branch_id 17592186045602,
;;     :file_id 17592186045606,
;;     :store_id 17592186045604}
;;    {:deltas {0 [nil ""], 1 [0 [:string/insert [0 "f"]]]},
;;     :timeline
;;     {0 #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}})
;;  (vims/add-change
;;   {{:user_id 17592186045568,
;;     :vims_id 17592186045603,
;;     :branch_id 17592186045602,
;;     :file_id 17592186045606,
;;     :store_id 17592186045604}
;;    {:deltas {2 [1 [:cursor/move [1]]]},
;;     :timeline
;;     {7
;;      #{{:delta-id 2, :change-type :cursor/move, :change-amount 1}}}}})]

;; [(vims/add-change
;;   {{:user_id 17592186045568,
;;     :vims_id 17592186045603,
;;     :branch_id 17592186045602,
;;     :file_id 17592186045606,
;;     :store_id 17592186045604}
;;    {:deltas {3 [2 [:string/insert [1 "f"]]]},
;;     :timeline
;;     {1007
;;      #{{:delta-id 3,
;;         :change-type :string/insert,
;;         :change-amount 1}}}}})
;;  (vims/add-change
;;   {{:user_id 17592186045568,
;;     :vims_id 17592186045603,
;;     :branch_id 17592186045602,
;;     :file_id 17592186045606,
;;     :store_id 17592186045604}
;;    {:deltas {4 [3 [:cursor/move [2]]]},
;;     :timeline
;;     {1017
;;      #{{:delta-id 4, :change-type :cursor/move, :change-amount 1}}}}})]
