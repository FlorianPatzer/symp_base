from ftplib import FTP

host = '172.19.0.4'
host = 'localhost' 

print(10*"=")
print('FTP Address', host)
print(10*"=")

ftp = FTP(host)
ftp.set_debuglevel(2)
#ftp.set_pasv(False)
ftp.login()
print(ftp.retrlines('LIST'))

ftp.quit()
