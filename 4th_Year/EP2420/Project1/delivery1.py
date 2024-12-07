import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
from sklearn.preprocessing import normalize
from sklearn.preprocessing import minmax_scale
from sklearn.preprocessing import scale
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.neural_network import MLPRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import GridSearchCV
from scipy.stats import gaussian_kde
from timeit import default_timer as timer

XX = pd.read_csv("VoD flashcrowd - JNSM 2017/X.csv",
        converters={'TimeStamp': pd.Timestamp})
YY = pd.read_csv("VoD flashcrowd - JNSM 2017/Y.csv",
        converters={'TimeStamp': pd.Timestamp})

#XX = XX.iloc[:1000,:]
#YY = YY.iloc[:1000,:]

# Convert timestamps to timedeltas
XX.iloc[:,0] = (XX.iloc[:,0] - XX.iloc[0,0]) // pd.Timedelta('1s')
YY.iloc[:,0] = (YY.iloc[:,0] - YY.iloc[0,0]) // pd.Timedelta('1s')

X = XX.iloc[:,1:]
Y = YY.iloc[:,1:]

# 1.1
random.seed(0)
random_cols = [random.choice(X.columns) for i in range(10)]

print(X.shape)
print(Y.shape)
print

for col in random_cols:
    print("\\textit{" + col + "} & \\num{" + "{:.2e}".format(np.mean(X[col])) + "} & \\num{" + "{:.2e}".format(np.std(X[col])) + "} & \\num{" + "{:.2e}".format(np.max(X[col])) + "} & \\num{" + "{:.2e}".format(np.min(X[col])) + "} & \\num{" + "{:.2e}".format(np.percentile(X[col], 0.25)) + "} & \\num{" + "{:.2e}".format(np.percentile(X[col], 0.90)) + "} \\\\ \n \hline")

for col in ['DispFrames']:
    print("\\textit{" + col + "} & \\num{" + "{:.2e}".format(np.mean(Y[col])) + "} & \\num{" + "{:.2e}".format(np.std(Y[col])) + "} & \\num{" + "{:.2e}".format(np.max(Y[col])) + "} & \\num{" + "{:.2e}".format(np.min(Y[col])) + "} & \\num{" + "{:.2e}".format(np.percentile(Y[col], 0.25)) + "} & \\num{" + "{:.2e}".format(np.percentile(Y[col], 0.90)) + "} \\\\ \n \hline")

# 1.2

# a)
L2X_col = normalize(X, norm='l2', axis=0)
L2X_row = normalize(X, norm='l2', axis=1)

# b)
RIX_col = minmax_scale(X)
RIX_row = minmax_scale(X.T).T

# c)
SX_col = scale(X, axis=0)
SX_row = scale(X, axis=1)

# 2.1.1, 2.1.2, 2.1.3, 2.1.4
X_train, X_test, Y_train, Y_test = train_test_split(XX, YY, test_size=0.3, shuffle=True)

X_train = X_train.sort_values(by=['TimeStamp'])
X_test = X_test.sort_values(by=['TimeStamp'])
Y_train = Y_train.sort_values(by=['TimeStamp'])
Y_test = Y_test.sort_values(by=['TimeStamp'])

lin_reg = LinearRegression(n_jobs=-1)
start = timer()
lin_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
end = timer()
lin_y = lin_reg.predict(X_test.iloc[:,1:])
print(mean_absolute_error(Y_test['DispFrames'], lin_y) / np.mean(Y_test['DispFrames']))
print(end-start)

rf_reg = RandomForestRegressor(n_jobs=-1, n_estimators=10)
start = timer()
rf_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
end = timer()
rf_y = rf_reg.predict(X_test.iloc[:,1:])
print(mean_absolute_error(Y_test['DispFrames'], rf_y) / np.mean(Y_test['DispFrames']))
print(end-start)

mlp_reg = MLPRegressor(max_iter=1000, activation='logistic', hidden_layer_sizes=(10,10))
start = timer()
mlp_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
end = timer()
mlp_y = mlp_reg.predict(X_test.iloc[:,1:])
print(mean_absolute_error(Y_test['DispFrames'], mlp_y) / np.mean(Y_test['DispFrames']))
print(end-start)

# 2.1.5
naive_estimation = np.mean(Y_train['DispFrames'])

# 2.1.6
pltlimit=3600
plt.plot(X_test[X_test['TimeStamp']<=pltlimit].iloc[:,0], [naive_estimation for i in X_test[X_test['TimeStamp']<=pltlimit].iloc[:,0]], 'g-', label="Naive estimation")
plt.plot(X_test[X_test['TimeStamp']<=pltlimit].iloc[:,0], Y_test[Y_test['TimeStamp']<=pltlimit]['DispFrames'], 'y-', label="Measured")
plt.plot(X_test[X_test['TimeStamp']<=pltlimit].iloc[:,0], rf_y[X_test['TimeStamp']<=pltlimit], 'r-', label="Estimation")
plt.axis('tight')
plt.xlabel('Time index')
plt.ylabel('Displayed frames')
plt.legend(loc='upper right', frameon=True)
plt.savefig('rfr.pdf')
plt.clf()

#2.1.7
density = gaussian_kde(Y_test['DispFrames'])
xs = np.linspace(0,26,800)
plt.plot(xs, density(xs))
plt.xlabel('Displayed frames')
plt.ylabel('Probability density')
plt.savefig('frames_density.pdf')
plt.clf()

plt.hist(Y_test['DispFrames'], bins=[i for i in range(1,26)])
plt.xlabel('Displayed frames')
plt.ylabel('Absolute frequency')
plt.savefig('frames_hist.pdf')
plt.clf()

errors = [(Y_test['DispFrames'].iloc[i] - rf_y[i]) for i in range(len(rf_y))]
density = gaussian_kde(errors)
xs = np.linspace(min(errors)-1,max(errors)+1,800)
plt.plot(xs, density(xs))
plt.xlabel('Estimation error')
plt.ylabel('Probability density')
plt.savefig('error_density.pdf')
plt.clf()
