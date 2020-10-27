import sys

sys.path.append('/home/ptz/workspace/SyMPFramework/x2owl')
import x2owl

x2o = x2owl.X2Owl("configurations/config_DEMO_controlstation.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_cloudserver.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_pfsense1.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_pfsense2.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_RevPi.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_sql_server.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_workstation1.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_workstation2.yml")
x2o.run()

x2o = x2owl.X2Owl("configurations/config_DEMO_workstation3.yml")
x2o.run()
