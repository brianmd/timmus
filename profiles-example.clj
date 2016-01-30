; Copy to profiles.clj and edit
{
 :profiles/dev
  {:env
   {:database-url "jdbc:mysql://localhost:3306/dbname?user=username&password=password"

   :db
    {:blue-harvest
     {
      :default :local
      :local
               {:classname "com.mysql.jdbc.Driver"                   ; must be in classpath
                :subprotocol "mysql"
                :subname "//localhost:3306/dbname"
                :host "localhost"
                :port 3306
                :dbname "dbname"
                :user "username"
                :password "password"
                }
      :dev
               {:classname "com.mysql.jdbc.Driver"                   ; must be in classpath
                :subprotocol "mysql"
                :subname "//localhost:3306/dbname"
                :host "localhost"
                :port 3306
                :name "dbname"
                :user "username"
                :password "password"
                }
      :neo
               {:classname "com.mysql.jdbc.Driver"
                :subprotocol "mysql"
                :subname "//neo:3306/dbname"
                :host "neo"
                :port 3306
                :name "dbname"
                :user "username"
                :password "password"
                }
      }
     }

   :papichulo
    {
     :username "username"
     :password "password"
     }
   }
   }

 :profiles/test
  {:env
   {:database-url "jdbc:mysql://localhost:3306/blue_harvest_test?user=username&password=password"}
   }
 }

