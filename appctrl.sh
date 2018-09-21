#!/bin/bash
serviceName=$(basename `pwd`)
product=newpay
targetName=${product}_${serviceName}
rootDir=$HOME

if [ -n "$1" ]; then
	case $1 in
		"start")
			if [ -n "$2" ]; then
				case $2 in
					"dev"|"test"|"pre")
						servicePort=8356
						echo "*** Starting $targetName, using profile: $2 ***"
						git checkout master
						git pull
						mvn clean package -Dmaven.test.skip=true
						cp target/${serviceName}*.jar ${rootDir}/bin/${targetName}.jar
						cd ${rootDir}/bin
						java -server -Xmx512m -jar ${targetName}.jar --server.port=${servicePort} --spring.profiles.active=$2 >/dev/null 2>&1 &
						tailf ${rootDir}/logs/${targetName}/service.log|sed '/Started Application/Q'
						echo "*** Started $targetName ***"
						exit 0
					;;
					"online")
						servicePort=8357
						echo "*** Starting $targetName, using profile: $2 ***"
						git checkout master
						git pull
						mvn clean package -Dmaven.test.skip=true
						cp target/${serviceName}*.jar ${rootDir}/bin/${targetName}_online.jar
						cd ${rootDir}/bin
						java -server -Xmx512m -jar ${targetName}_online.jar --server.port=${servicePort} --spring.profiles.active=$2 >/dev/null 2>&1 &
						tailf ${rootDir}/logs/${targetName}/service.log|sed '/Started Application/Q'
						echo "*** Started $targetName ***"
						exit 0
					;;
					*) echo "profile:dev test pre online is available."
				esac
			else
				echo "profile is required."
			fi
		;;

		"stop")
			if [ -n "$2" ]; then
				echo "Shutting down $targetName, pls wait."
				case $2 in
					"dev"|"test"|"pre")
					servicePort=8356
					;;
					"online")
					servicePort=8357
					targetName=${targetName}_online
					;;
					*) echo "profile:dev test pre online is available."
				esac
				curl -X POST http://ops:pwd@localhost:${servicePort}/actuator/shutdown
				echo
				for i in `seq 1 10`
				do
					PID=`ps -ef|grep ${targetName}|grep -v grep|awk '{print $2}'`
					if [ -z "${PID}" ]; then
			    		echo "${targetName} shutdown gracefully."
			    		SHUTDOWN=1
			    		break
			   		else
			   			echo "waiting ${targetName} shutdown ${i}"
				    	sleep 1
			    	fi
				done
				
				if [ -z "${SHUTDOWN}" ]; then
					echo "app may not shutdown gracefully, kill immediately."
					for i in `seq 11 30`
					do
					   	PID=`ps -ef|grep ${targetName}|grep -v grep|awk '{print $2}'`
					    if [ -z "${PID}" ]; then
					        break;
					    else
					       	kill -9 ${PID}
					        echo "waiting ${targetName} shutdown: ${i}"
					        sleep 1
					    fi
					done
					echo "${targetName} shutdown succefully."
				fi
				
				exit 0
			else
				echo "profile is required."
			fi
		;;
		
		*) echo "appctrl (start, stop) profile"
	esac
else
    echo "appctrl (start, stop) profile"
fi
