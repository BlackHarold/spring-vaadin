
## Сервисы 
на примере DEV35. На всех остальных серверах +- по образу и подобию.

Работают в основном с двумя важными сервисами - 3DSpace и Exalead. Все сервисы запускаются через службы systemd. Поэтому напрямую дергать томкат не нужно.
Конфигурация сервисов располагается:
```Shell
cat /etc/systemd/system/имя службы
```

### 3DSpaceCAS
```Shell
sudo systemctl start 3DSpaceCAS.service
sudo systemctl stop 3DSpaceCAS.service
sudo systemctl restart 3DSpaceCAS.service
```
Логи: 
```Shell
sudo systemctl status 3DSpaceCAS.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u 3DSpaceCAS #логи службы в системном журнале

cd /app/R2020x/3DSpace/linux_a64/code/tomee/logs/ #логи Tomcat 3DSpaceCAS
cat /app/R2020x/3DSpace/linux_a64/code/tomee/logs/catalina.out
```

### 3DSpaceNoCAS
```Shell
sudo systemctl start 3DSpaceNoCAS.service
sudo systemctl stop 3DSpaceNoCAS.service
sudo systemctl restart 3DSpaceNoCAS.service
```
Логи: 
```Shell
sudo systemctl status 3DSpaceNoCAS.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u 3DSpaceNoCAS #логи службы в системном журнале

cd /app/R2020x/3DSpace/linux_a64/code/tomeeNoCAS/logs/ #логи Tomcat 3DSpaceNoCAS
cat /app/R2020x/3DSpace/linux_a64/code/tomeeNoCAS/logs/catalina.out
```

### Exalead (3DSpaceIndex)
```Shell
sudo systemctl start 3DSpaceIndex.service
sudo systemctl stop 3DSpaceIndex.service
sudo systemctl restart 3DSpaceIndex.service
```
Логи: 
```Shell
sudo systemctl status 3DSpaceIndex.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u 3DSpaceIndex #логи службы в системном журнале
```
==логирование Tomcat 3DSpaceIndex не включено==

Если сервис не стартует из-за невозможности получить статус CloudView, необходимо проверить сервис на сервере, где установлен Exalead:
```Shell
cd /app/R2020x/3DSpaceIndex/linux_a64/cv/data/bin/
./cvinit.sh status #(получение статуса)
./cvinit.sh start
```
Далее вернуться к запуску службы**3DSpaceIndex.service**

### Federated Search (FedSearch)
Если при Поиске в приложении выдается ошибка связанная с federated search, то нужно и эту службу перезапустить.
```Shell
sudo systemctl start FedSearch.service
sudo systemctl stop FedSearch.service
sudo systemctl restart FedSearch.service
```
Логи: 
```Shell
sudo systemctl status FedSearch.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u FedSearch #логи службы в системном журнале

cd /app/R2020x/FedSearch/linux_a64/code/tomee/logs/ #логи Tomcat FedSearch
cat /app/R2020x/FedSearch/linux_a64/code/tomee/logs/catalina.out
```

### 3DDashboard
```Shell
sudo systemctl start 3DDashboard.service
sudo systemctl stop 3DDashboard.service
sudo systemctl restart 3DDashboard.service
```
Логи: 
```Shell
sudo systemctl status 3DDashboard.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u 3DDashboard #логи службы в системном журнале
```
==логирование Tomcat 3DDashboard не включено==

### FCS 
```Shell
sudo systemctl start FCS.service
sudo systemctl stop FCS.service
sudo systemctl restart FCS.service
```
Логи: 
```Shell
sudo systemctl status FCS.service #(ключ -l вывод расширенного лога. Сначала запрос без ключа -l, потом с ключем, иначе выдаёт ошибку)

journalctl -u FCS.service #логи службы в системном журнале

cd /app/R2020x/FCS/linux_a64/code/tomee/logs/ #логи Tomcat 3DSpaceCAS
cat /app/R2020x/FCS/linux_a64/code/tomee/logs/catalina.out
```
