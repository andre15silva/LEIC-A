%E = csvread('example1.dat');
E = csvread('example2.dat');
%k =4; % For example 1
k = 2; % For example 2


% Step 1
col1 = E(:,1);
col2 = E(:,2);
max_ids = max(max(col1,col2));
As = sparse(col1, col2, 1, max_ids, max_ids); 
A = full(As);
G = graph(A);

% Visualize G
plot(G);

% Step 2
D = diag(sum(A,2));
L = (D^(-0.5)) * (A) * (D^(-0.5));

% Step 3
[X, D] = eigs(L, k, 'la');

% Step 4
Y = X./(sum(X.^2,2).^(0.5));

% Step 5
idx = kmeans(Y,k);

% Step 6
figure;
hold on;
p = plot(G);
colors = ['r', 'b', 'g', 'y'];
for i = 1:k
    highlight(p,find(idx==i),'NodeColor', colors(:,i));
end

% Plot fiedler vector 

figure(3);
Lap = laplacian(G);
[VL,DL] = eigs(Lap,k,'smallestabs');

for i = 1:k
    disp(i);
    w = VL(:,i);
    sW = sort(w);
    plot(sW, "DisplayName", "" + i);
%     legend(i);
    hold on;
end

legend;
title("Sorted fiedler vector: k="+k);

% Plot sparsity pattern

figure, image(A * 250 / k);
title("Sparsity pattern");
