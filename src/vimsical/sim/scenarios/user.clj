(ns vimsical.sim.scenarios.user)

;; * Existing vims

{:xhr-query
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
      [:db/id :settings/record-audio? :settings/playback-speed]}
     {:user/memory
      [:db/id
       {:memory/vims-memories
        [:db/id
         {:vims-memory/vims
          [:db/id
           :vims/title
           {:vims/owner [:db/id :user/first-name :user/last-name]}]}
         :vims-memory/shown-files]}]}]}],
 :resp
 '{:app/user
   {:db/id 17592186045547,
    :user/vimsae
    [{:db/id 17592186045537,
      :vims/title "Untitled",
      :vims/owner {:db/id 17592186045547},
      :vims/master-branch
      {:db/id 17592186045538,
       :branch/start-time 0,
       :branch/author {:db/id 17592186045547},
       :branch/stores
       [{:db/id 17592186045539,
         :store/file
         {:db/id 17592186045540, :file/content-type "text/html"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id 17592186045541,
         :store/file
         {:db/id 17592186045542, :file/content-type "text/css"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id 17592186045543,
         :store/file
         {:db/id 17592186045544, :file/content-type "text/javascript"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id 17592186045545,
         :store/file
         {:db/id 17592186045546, :file/content-type "ui/pointer"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0,
             :change-amount 0,
             :change-type :branch/new}}}}]},
      :vims/branches
      [{:db/id 17592186045538,
        :branch/start-time 0,
        :branch/author {:db/id 17592186045547},
        :branch/stores
        [{:db/id 17592186045539,
          :store/file
          {:db/id 17592186045540, :file/content-type "text/html"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}
         {:db/id 17592186045541,
          :store/file
          {:db/id 17592186045542, :file/content-type "text/css"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}
         {:db/id 17592186045543,
          :store/file
          {:db/id 17592186045544, :file/content-type "text/javascript"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}
         {:db/id 17592186045545,
          :store/file
          {:db/id 17592186045546, :file/content-type "ui/pointer"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}]}]}
     {:db/id 17592186045552,
      :vims/title "Untitled",
      :vims/owner {:db/id 17592186045547},
      :vims/master-branch
      {:db/id 17592186045553,
       :branch/start-time 0,
       :branch/author {:db/id 17592186045547},
       :branch/stores
       [{:db/id 17592186045554,
         :store/file
         {:db/id 17592186045555, :file/content-type "text/html"},
         :store/deltas
         {:deltas
          {0 [nil ""],
           1 [0 [:string/insert [0 "f"]]],
           2 [1 [:cursor/move [1]]]},
          :start-delta-id 0,
          :latest-delta-id 2,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}},
          1004
          #{{:delta-id 1,
             :change-type :string/insert,
             :change-amount 1}},
          1020
          #{{:delta-id 2,
             :change-type :cursor/move,
             :change-amount 1}}}}
        {:db/id 17592186045556,
         :store/file
         {:db/id 17592186045557, :file/content-type "text/css"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id 17592186045558,
         :store/file
         {:db/id 17592186045559, :file/content-type "text/javascript"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id 17592186045560,
         :store/file
         {:db/id 17592186045561, :file/content-type "ui/pointer"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0,
             :change-amount 0,
             :change-type :branch/new}}}}]},
      :vims/branches
      [{:db/id 17592186045553,
        :branch/start-time 0,
        :branch/author {:db/id 17592186045547},
        :branch/stores
        [{:db/id 17592186045554,
          :store/file
          {:db/id 17592186045555, :file/content-type "text/html"},
          :store/deltas
          {:deltas
           {0 [nil ""],
            1 [0 [:string/insert [0 "f"]]],
            2 [1 [:cursor/move [1]]]},
           :start-delta-id 0,
           :latest-delta-id 2,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0, :change-amount 0, :change-type :branch/new}},
           1004
           #{{:delta-id 1,
              :change-type :string/insert,
              :change-amount 1}},
           1020
           #{{:delta-id 2,
              :change-type :cursor/move,
              :change-amount 1}}}}
         {:db/id 17592186045556,
          :store/file
          {:db/id 17592186045557, :file/content-type "text/css"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}
         {:db/id 17592186045558,
          :store/file
          {:db/id 17592186045559, :file/content-type "text/javascript"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}
         {:db/id 17592186045560,
          :store/file
          {:db/id 17592186045561, :file/content-type "ui/pointer"},
          :store/deltas
          {:deltas {0 [nil ""]},
           :start-delta-id 0,
           :latest-delta-id 0,
           :cache {},
           :cache-countdown 1000},
          :store/timeline
          {0
           #{{:delta-id 0,
              :change-amount 0,
              :change-type :branch/new}}}}]}]}],
    :user/memory
    {:db/id 17592186045563,
     :memory/vims-memories
     [{:db/id 17592186045564,
       :vims-memory/vims
       {:db/id 17592186045552,
        :vims/title "Untitled",
        :vims/owner {:db/id 17592186045547}}}]}}}}

;; * New vims

{:xhr-query
 '[(vims/new
    {:db/id #om/id["33c13e52-8263-4bb8-9cb8-2f97036a41a8"],
     :vims/owner [:db/id 17592186045547],
     :vims/title "Untitled",
     :vims/cast? false,
     :vims/master-branch
     {:db/id #om/id["6f3292d5-7e5c-47ef-b9f9-21a0cfe42373"],
      :branch/title "Master",
      :branch/author [:db/id 17592186045547],
      :branch/creation-time 0,
      :branch/start-time 0,
      :branch/stores
      [{:db/id #om/id["fe85d18e-eaa0-42ae-bcff-a1a516d9c8cd"],
        :store/file
        {:db/id #om/id["02b83519-8cb3-4dcc-93bc-6d77672a3068"],
         :file/content-type "text/html"},
        :store/deltas
        {:deltas {0 [nil ""]},
         :start-delta-id 0,
         :latest-delta-id 0,
         :cache {},
         :cache-countdown 1000},
        :store/timeline
        {0
         #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
       {:db/id #om/id["6c29921c-c953-4e09-836f-8783507cc18b"],
        :store/file
        {:db/id #om/id["3d21b1f9-0b39-466f-a823-01a785b7ff2e"],
         :file/content-type "text/css"},
        :store/deltas
        {:deltas {0 [nil ""]},
         :start-delta-id 0,
         :latest-delta-id 0,
         :cache {},
         :cache-countdown 1000},
        :store/timeline
        {0
         #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
       {:db/id #om/id["81040ef6-28eb-4ef9-a2bf-8751107a9a08"],
        :store/file
        {:db/id #om/id["513a808c-fdc6-4455-8a2a-282081a7e5f6"],
         :file/content-type "text/javascript"},
        :store/deltas
        {:deltas {0 [nil ""]},
         :start-delta-id 0,
         :latest-delta-id 0,
         :cache {},
         :cache-countdown 1000},
        :store/timeline
        {0
         #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
       {:db/id #om/id["6d4e9fd2-631e-49af-ba98-c5d33acb38fe"],
        :store/file
        {:db/id #om/id["5836a7cd-3e68-4f22-8c02-2d9eca477a92"],
         :file/content-type "ui/pointer"},
        :store/deltas
        {:deltas {0 [nil ""]},
         :start-delta-id 0,
         :latest-delta-id 0,
         :cache {},
         :cache-countdown 1000},
        :store/timeline
        {0
         #{{:delta-id 0,
            :change-amount 0,
            :change-type :branch/new}}}}]},
     :vims/branches
     [{:db/id #om/id["6f3292d5-7e5c-47ef-b9f9-21a0cfe42373"],
       :branch/title "Master",
       :branch/author [:db/id 17592186045547],
       :branch/creation-time 0,
       :branch/start-time 0,
       :branch/stores
       [{:db/id #om/id["fe85d18e-eaa0-42ae-bcff-a1a516d9c8cd"],
         :store/file
         {:db/id #om/id["02b83519-8cb3-4dcc-93bc-6d77672a3068"],
          :file/content-type "text/html"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id #om/id["6c29921c-c953-4e09-836f-8783507cc18b"],
         :store/file
         {:db/id #om/id["3d21b1f9-0b39-466f-a823-01a785b7ff2e"],
          :file/content-type "text/css"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id #om/id["81040ef6-28eb-4ef9-a2bf-8751107a9a08"],
         :store/file
         {:db/id #om/id["513a808c-fdc6-4455-8a2a-282081a7e5f6"],
          :file/content-type "text/javascript"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0, :change-amount 0, :change-type :branch/new}}}}
        {:db/id #om/id["6d4e9fd2-631e-49af-ba98-c5d33acb38fe"],
         :store/file
         {:db/id #om/id["5836a7cd-3e68-4f22-8c02-2d9eca477a92"],
          :file/content-type "ui/pointer"},
         :store/deltas
         {:deltas {0 [nil ""]},
          :start-delta-id 0,
          :latest-delta-id 0,
          :cache {},
          :cache-countdown 1000},
         :store/timeline
         {0
          #{{:delta-id 0,
             :change-amount 0,
             :change-type :branch/new}}}}]}]})
   (vims/add-memory
    {:memory
     {:db/id #om/id["5c4d58d6-9517-4ab3-bc2d-753386f46430"],
      :memory/vims-memories
      [{:db/id #om/id["605d086d-243c-4b67-b237-6e5e6ab16dff"],
        :vims-memory/vims
        [:db/id #om/id["33c13e52-8263-4bb8-9cb8-2f97036a41a8"]]}]}})],
 :resp
 '{vims/new
   {:result
    {:tempids
     {[:db/id #om/id["513a808c-fdc6-4455-8a2a-282081a7e5f6"]]
      [:db/id 17592186045573],
      [:db/id #om/id["5836a7cd-3e68-4f22-8c02-2d9eca477a92"]]
      [:db/id 17592186045575],
      [:db/id #om/id["fe85d18e-eaa0-42ae-bcff-a1a516d9c8cd"]]
      [:db/id 17592186045568],
      [:db/id #om/id["6f3292d5-7e5c-47ef-b9f9-21a0cfe42373"]]
      [:db/id 17592186045567],
      [:db/id #om/id["6d4e9fd2-631e-49af-ba98-c5d33acb38fe"]]
      [:db/id 17592186045574],
      [:db/id #om/id["6c29921c-c953-4e09-836f-8783507cc18b"]]
      [:db/id 17592186045570],
      [:db/id #om/id["3d21b1f9-0b39-466f-a823-01a785b7ff2e"]]
      [:db/id 17592186045571],
      [:db/id #om/id["33c13e52-8263-4bb8-9cb8-2f97036a41a8"]]
      [:db/id 17592186045566],
      [:db/id #om/id["81040ef6-28eb-4ef9-a2bf-8751107a9a08"]]
      [:db/id 17592186045572],
      [:db/id #om/id["02b83519-8cb3-4dcc-93bc-6d77672a3068"]]
      [:db/id 17592186045569]}}},
   vims/add-memory
   {:result
    {:tempids
     {[:db/id #om/id["33c13e52-8263-4bb8-9cb8-2f97036a41a8"]]
      [:db/id 17592186045566],
      [:db/id #om/id["5c4d58d6-9517-4ab3-bc2d-753386f46430"]]
      [:db/id 17592186045577],
      [:db/id #om/id["605d086d-243c-4b67-b237-6e5e6ab16dff"]]
      [:db/id 17592186045578]}}}}}

;; * Changes

{:ws-query
 '[(vims/add-change
    {{:user [:db/id 17592186045547],
      :vims [:db/id 17592186045552],
      :branch [:db/id 17592186045553],
      :file [:db/id 17592186045555],
      :store [:db/id 17592186045554]}
     {:deltas {1 [0 [:string/insert [0 "f"]]]},
      :timeline
      {1004
       #{{:delta-id 1,
          :change-type :string/insert,
          :change-amount 1}}}}})
   (vims/add-change
    {{:user [:db/id 17592186045547],
      :vims [:db/id 17592186045552],
      :branch [:db/id 17592186045553],
      :file [:db/id 17592186045555],
      :store [:db/id 17592186045554]}
     {:deltas {2 [1 [:cursor/move [1]]]},
      :timeline
      {1020
       #{{:delta-id 2,
          :change-type :cursor/move,
          :change-amount 1}}}}})],
 :resp '{vims/add-change {:result ()}}}
