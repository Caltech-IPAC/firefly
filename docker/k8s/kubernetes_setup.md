## Setup kubernetes using kubeadm
Installation: https://kubernetes.io/docs/setup/independent/install-kubeadm/

    Kubernetes: v1.8.5
    Docker: v17.03.2


    VMs: irsawebdev9,10,11,12
    Debian 8
    irsawebdev9 -> master
    others -> nodes (workers)

issues specific to these machines:

1. CGROUPS_MEMORY: missing
Resolved by enable cgroup memory subsystem in debian, it is disabled defaultly in debian.

        $ vi /etc/default/grub
            GRUB_CMDLINE_LINUX="cgroup_enable=memory”
        $ update-grub 
        $ reboot
    
2. need to turn off swap

        $ swapoff -a


### Install Docker  

recommended version as of 1/9/2017:  v17.03

    $ apt-get update
    $ apt-get install -y \
        apt-transport-https \
        ca-certificates \
        curl \
        software-properties-common
        
    $ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -
    $ add-apt-repository \
       "deb https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
       $(lsb_release -cs) \
       stable"
       
    $ apt-get update && apt-get install -y docker-ce=$(apt-cache madison docker-ce | grep 17.03 | head -1 | awk '{print $3}')


**NOTE:** This is no longer true in k8s v1.9.  I had to remove this step to get kubeadm to init.  However, it is default to cgroupfs instead of systemd.  
If systemd is desired, you need to edit kubelet start up parameters and  apply below step.

To ensure that the cgroup driver used by kubelet is the same as the one used by Docker.

	$ cat << EOF > /etc/docker/daemon.json
	{
	  "exec-opts": ["native.cgroupdriver=systemd"]
	}
	EOF
	restart Docker
		$ systemctl stop/start docker

### To manage Docker as a non-root user

create docker group then add irsadmin to it.

    $ sudo groupadd docker

    $ sudo usermod -aG docker irsadmin



### Installing kubeadm, kubelet and kubectl

    $ apt-get update && apt-get install -y apt-transport-https
    $ curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -    
    $ cat <<EOF >/etc/apt/sources.list.d/kubernetes.list
      deb http://apt.kubernetes.io/ kubernetes-xenial main
      EOF
    $ apt-get update
    $ apt-get install -y kubelet=1.8.5-00 kubeadm=1.8.5-00 kubectl=1.8.5-00			# install version 1.8.5

*** Repeat the above docker and kubeadm installation on all machines in the cluster



### Create the cluster using kubeadm

https://kubernetes.io/docs/setup/independent/create-cluster-kubeadm/

starting in 1.8, tokens expire after 24 hours by default (if you require a non-expiring token use --token-ttl 0)
	
    $ sudo kubeadm init --token-ttl 0
        NOTES: below is output from successful init.  Save the join statement.  It's needed to add nodes later in the process
        You can now join any number of machines by running the following on each node
        as root:
            kubeadm join --token ae1f3c.9b9ae991b1531a02 134.4.70.136:6443 --discovery-token-ca-cert-hash sha256:b10fd95c4f78b788fc435803b71281838ad7b1d4e0b286b716365fd32d9c4e87

### To make kubectl work for your non-root user

    $ mkdir -p $HOME/.kube
    $ sudo cp /etc/kubernetes/admin.conf $HOME/.kube/config
    $ sudo chown $(id -u):$(id -g) $HOME/.kube/config			# was not own by irsadmin

copy config into any machines you wish to allow kubectl access

	$ scp $HOME/.kube/config irsawebdev10:.kube/
	$ scp $HOME/.kube/config irsawebdev11:.kube/


### Adding nodes to the cluster

On each machine you want to join the cluster, run:
    
    $ sudo kubeadm join <command-generated-by-kubeadm-init-from-above>
    

Before continuing, make sure all pod are up and running.  It's normal for dns pod to be pending.

    $ kubectl get --all-namespaces -o wide pods
        NAMESPACE     NAME                                  READY     STATUS    RESTARTS   AGE       IP             NODE
        kube-system   etcd-irsawebdev9                      1/1       Running   0          12s       134.4.70.136   irsawebdev9
        kube-system   kube-apiserver-irsawebdev9            1/1       Running   0          18s       134.4.70.136   irsawebdev9
        kube-system   kube-controller-manager-irsawebdev9   1/1       Running   0          11s       134.4.70.136   irsawebdev9
        kube-system   kube-dns-545bc4bfd4-qj79v             0/3       Pending   0          1m        <none>         <none>
        kube-system   kube-proxy-pfrgg                      1/1       Running   0          1m        134.4.70.136   irsawebdev9
        kube-system   kube-scheduler-irsawebdev9            1/1       Running   0          8s        134.4.70.136   irsawebdev9


### Install Weave Net pod network

https://www.weave.works/docs/net/latest/kube-addon/

    $ kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"


** Skip this part (Master Isolation)

To schedule pods on the master, e.g. a single-machine Kubernetes cluster for development, run:

    $ kubectl taint nodes --all node-role.kubernetes.io/master-


Again, make sure all pod are up and running before moving to the next step.  At this time, dns should be running.

    $ kubectl get --all-namespaces -o wide pods

All nodes should be in 'Ready' status before continuing to next step

    $ kubectl get nodes
            NAME           STATUS    ROLES     AGE       VERSION
            irsawebdev10   Ready     <none>    1m        v1.8.5
            irsawebdev11   Ready     <none>    1m        v1.8.5
            irsawebdev9    Ready     master    6m        v1.8.5


### Cleanup .. do this to start over (rerun kubeadm init)

    $ kubectl drain irsawebdev9  --delete-local-data --force --ignore-daemonsets
    $ kubectl drain irsawebdev10 --delete-local-data --force --ignore-daemonsets
    $ kubectl drain irsawebdev11 --delete-local-data --force --ignore-daemonsets
    $ kubectl delete node irsawebdev9 irsawebdev10 irsawebdev11
    
then, on each node, run:

    $ sudo kubeadm reset



### Deploy firefly into the cluster


Source files located at `firefly.git:/firefly/docker/k8s/`
These yaml files combine all related configurations into one.
The one file may contains Namespace, ConfigMap, Deployment, Service, and etc.


#### Setup nginx ingress controller:
This is a replacement for our apache proxy.  It will provide load-balancing as well as https access

    $ kubectl apply -f ./nginx-controller.yaml
    $ kubectl apply -f ./nginx-svc.yaml
	
Again, wait until the ngnix pods are up and running before moving to the next step

    $ kubectl get -n ingress-nginx pods
        NAMESPACE       NAME                                       READY     STATUS    RESTARTS   AGE       IP             NODE
        ingress-nginx   default-http-backend-66b447d9cf-vvjz4      1/1       Running   0          1m        10.36.0.0      irsawebdev10
        ingress-nginx   nginx-ingress-controller-8dcfb95b9-dh62c   1/1       Running   0          1m        10.46.0.0      irsawebdev11
	
#### Setup firefly:

	$ kubectl apply -f ./firefly.yaml

Verify that firefly is running by going to:  https://irsawebdev9.ipac.caltech.edu/firefly/	



### Setup Dashboard UI

Setup Dashboard

	$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/master/src/deploy/recommended/kubernetes-dashboard.yaml

Create admin-user account	

	$ kubectl apply -f dashboard-users.yaml

To display the account bearer token.  This is used to log into Dashboard UI.

	$ kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | grep admin-user | awk '{print $1}')
	


### To gain access to the cluster from your laptop via kubectl

Install kubectl(on OS X) using Homebrew:

	$ brew install kubectl

Copy KubeConfig from cluster:

	$ scp irsadmin@irsawebdev9:.kube/config .kube/


From your localhost(laptop):

	$ kubectl proxy

Now you can access Dashboard at: http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/	
Log in using the admin-user token from above



### To update firefly

Build updated firefly and push image to DockerHub

    $ gradle -Penv=dev :firefly:war
    $ gradle -Pdocker_tag=k8s :firefly:dockerPublish            // this will create a docker image(dockerImage) witht the tag, then push it to DockHub

Edit firefly.yaml with the updated firefly version.  Then,

	$ kubectl apply -f ./firefly.yaml
	$ kubectl get pods		

During this rolling update, you should be able to see that the old pods shutting down, while new ones starting up.





### Useful commands
-------------------

display the logs  of the given pod

    $ kubectl logs <pod name> [-n <name space>] [-c <container name if more than one>]

list the pods, output internal IP and node name

    $ kubectl get pods -o wide [-n <name space> | --all-namespaces]

Enter the container's bash shell

    $ kubectl exec -ti <pod-name> [-n <name space>] -- /bin/bash



