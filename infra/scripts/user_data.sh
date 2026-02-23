#!/bin/bash
set -e

# 로그 기록
exec > >(tee /var/log/user-data.log) 2>&1
echo "Starting user data script..."

# 시스템 업데이트
apt-get update
apt-get upgrade -y

# 공통 의존성 설치
apt-get install -y ca-certificates curl gnupg lsb-release apt-transport-https

# 0. SSM Agent 설치
echo "Installing SSM Agent..."
snap install amazon-ssm-agent --classic
systemctl enable snap.amazon-ssm-agent.amazon-ssm-agent.service
systemctl start snap.amazon-ssm-agent.amazon-ssm-agent.service

# 1. Docker 설치
echo "Installing Docker..."
mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

systemctl enable docker
systemctl start docker
usermod -aG docker ubuntu

# 2. Kubernetes 설치 (v1.34)
echo "Installing Kubernetes..."

cat <<EOF | tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

modprobe overlay
modprobe br_netfilter

cat <<EOF | tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sysctl --system

mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml
systemctl restart containerd

curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.34/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.34/deb/ /' | tee /etc/apt/sources.list.d/kubernetes.list

apt-get update
apt-get install -y kubelet kubeadm kubectl
apt-mark hold kubelet kubeadm kubectl
systemctl enable kubelet

# 3. Helm 설치 (v4.1.1)
echo "Installing Helm..."
curl -fsSL https://get.helm.sh/helm-v4.1.1-linux-amd64.tar.gz -o /tmp/helm.tar.gz
tar -zxvf /tmp/helm.tar.gz -C /tmp
mv /tmp/linux-amd64/helm /usr/local/bin/helm
rm -rf /tmp/helm.tar.gz /tmp/linux-amd64

# 4. 클러스터 초기화
echo "Initializing Kubernetes cluster..."
kubeadm init --pod-network-cidr=10.244.0.0/16

# kubectl 설정
mkdir -p /home/ubuntu/.kube
cp -i /etc/kubernetes/admin.conf /home/ubuntu/.kube/config
chown -R ubuntu:ubuntu /home/ubuntu/.kube

mkdir -p /root/.kube
cp -i /etc/kubernetes/admin.conf /root/.kube/config

id ssm-user &>/dev/null || useradd -m ssm-user
mkdir -p /home/ssm-user/.kube
cp /etc/kubernetes/admin.conf /home/ssm-user/.kube/config
chown -R ssm-user:ssm-user /home/ssm-user
chmod 600 /home/ssm-user/.kube/config

echo 'export KUBECONFIG=/etc/kubernetes/admin.conf' >> /etc/profile.d/kubeconfig.sh
echo 'export KUBECONFIG=/etc/kubernetes/admin.conf' >> /etc/environment
chmod +r /etc/kubernetes/admin.conf

# Control Plane에서도 Pod 실행 허용
export KUBECONFIG=/etc/kubernetes/admin.conf
kubectl taint nodes --all node-role.kubernetes.io/control-plane- || true

# Flannel CNI 설치
kubectl apply -f https://raw.githubusercontent.com/flannel-io/flannel/master/Documentation/kube-flannel.yml

# Local Path Provisioner 설치
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/v0.0.34/deploy/local-path-storage.yaml
sleep 10
kubectl patch storageclass local-path -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

# Ingress NGINX Controller 설치
echo "Installing Ingress NGINX Controller..."
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.kind=DaemonSet \
  --set controller.hostNetwork=true \
  --set controller.service.type=ClusterIP \
  --set controller.dnsPolicy=ClusterFirstWithHostNet
kubectl -n ingress-nginx rollout status daemonset/ingress-nginx-controller --timeout=180s

# Cert-Manager 설치
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.17.0/cert-manager.yaml

# 5. 방화벽 설정 (UFW)
echo "Configuring firewall..."
apt-get install -y ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 6443/tcp
ufw allow 10250/tcp
ufw allow 30000:32767/tcp
ufw --force enable

echo "User data script completed!"
