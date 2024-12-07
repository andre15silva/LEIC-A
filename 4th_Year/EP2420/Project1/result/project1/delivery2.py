import pandas as pd
import numpy as np
import random
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression
from sklearn.linear_model import Lasso
from sklearn.metrics import mean_absolute_error
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestRegressor
from sklearn.neural_network import MLPRegressor
from sklearn.preprocessing import normalize
from sklearn.preprocessing import minmax_scale
from sklearn.preprocessing import scale

XX = pd.read_csv("VoD flashcrowd - JNSM 2017/X.csv",
        converters={'TimeStamp': pd.Timestamp})
YY = pd.read_csv("VoD flashcrowd - JNSM 2017/Y.csv",
        converters={'TimeStamp': pd.Timestamp})

# Convert timestamps to timedeltas
XX.iloc[:,0] = (XX.iloc[:,0] - XX.iloc[0,0]) // pd.Timedelta('1s')
YY.iloc[:,0] = (YY.iloc[:,0] - YY.iloc[0,0]) // pd.Timedelta('1s')

# 2.2
sizes = [25, 50, 100, 200, 400, 800, 1600, 3200]
errors={i: [] for i in sizes}
for i in sizes:
    for j in range(50):
        X_train = XX.sample(i).sort_index()
        Y_train = YY[YY.index.isin(X_train.index)]
        X_test = XX[~XX.index.isin(X_train.index)].sample(1000).sort_index()
        Y_test = YY[YY.index.isin(X_test.index)]

        lin_reg = Lasso(alpha=0.1)
        lin_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
        lin_y = lin_reg.predict(X_test.iloc[:,1:])
        errors[i] += [mean_absolute_error(Y_test['DispFrames'], lin_y) / np.mean(Y_test['DispFrames'])]

nmaes = [np.mean(errors[i]) for i in sizes]
stds = [2*np.std(errors[i]) for i in sizes]

plt.errorbar(sizes, nmaes, yerr=stds, fmt='ob-', capsize=8)
plt.xlabel('Size of training set')
plt.ylabel('Normalized Mean Absolute Error')
plt.xscale('log', basex=2)
plt.savefig('training_set_size_errors.pdf')
plt.clf()

#XX = XX.iloc[:1000,:]
#YY = YY.iloc[:1000,:]

# 1.2

datasets = []

# a)
L2X_col = XX.copy()
L2X_col.iloc[:,1:] = normalize(L2X_col.iloc[:,1:], norm='l2', axis=0)
L2X_row = XX.copy()
L2X_row.iloc[:,1:] = normalize(L2X_row.iloc[:,1:], norm='l2', axis=1)

# b)
RIX_col = XX.copy()
RIX_col.iloc[:,1:] = minmax_scale(RIX_col.iloc[:,1:])
RIX_row = XX.copy()
RIX_row.iloc[:,1:] = minmax_scale(RIX_row.iloc[:,1:].T).T

# c)
SX_col = XX.copy()
SX_col.iloc[:,1:] = scale(SX_col.iloc[:,1:], axis=0)
SX_row = XX.copy()
SX_row.iloc[:,1:] = scale(SX_row.iloc[:,1:], axis=1)

# 3.1 and 3.2
errors = []
for dataset in [XX, L2X_col, L2X_row, RIX_col, RIX_row, SX_col, SX_row]:
    errors += [[]]
    X_train, X_test, Y_train, Y_test = train_test_split(dataset, YY, test_size=0.3, shuffle=True)
    X_train = X_train.sort_values(by=['TimeStamp'])
    X_test = X_test.sort_values(by=['TimeStamp'])
    Y_train = Y_train.sort_values(by=['TimeStamp'])
    Y_test = Y_test.sort_values(by=['TimeStamp'])

    lin_reg = Lasso(alpha=0.1)
    lin_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
    lin_y = lin_reg.predict(X_test.iloc[:,1:])
    errors[-1] += [mean_absolute_error(Y_test['DispFrames'], lin_y) / np.mean(Y_test['DispFrames'])]

    rf_reg = RandomForestRegressor(n_jobs=-1, n_estimators=10)
    rf_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
    rf_y = rf_reg.predict(X_test.iloc[:,1:])
    errors[-1] += [mean_absolute_error(Y_test['DispFrames'], rf_y) / np.mean(Y_test['DispFrames'])]

    mlp_reg = MLPRegressor(max_iter=1000, activation='logistic', hidden_layer_sizes=(10,10))
    mlp_reg.fit(X_train.iloc[:,1:], Y_train['DispFrames'])
    mlp_y = mlp_reg.predict(X_test.iloc[:,1:])
    errors[-1] += [mean_absolute_error(Y_test['DispFrames'], mlp_y) / np.mean(Y_test['DispFrames'])]

labels = ['Unprocessed', 'L2 Norm Col', 'L2 Norm Row', 'Res Int Col', 'Res Int Row', 'Std Col', 'Std Row']
lin_errors = [error[0] for error in errors]
rf_errors = [error[1] for error in errors]
mlp_errors = [error[2] for error in errors]
x = np.arange(len(labels))
width = 0.25

fig, ax = plt.subplots(figsize=(8,6))
ax.bar(x - 0.25, lin_errors, width, label='Linear Regressor (Lasso)')
ax.bar(x + 0.00, rf_errors, width, label='Random Forest Regressor')
ax.bar(x + 0.25, mlp_errors, width, label='MLP Regressor')
ax.set_ylabel('Normalized Mean Absolute Error')
ax.set_xticks(x)
ax.set_xticklabels(labels)
ax.tick_params(axis='x', which='major')
ax.legend()
fig.tight_layout()
plt.savefig('pre_processing_accuracies.pdf')
plt.clf()

# 3.3 and 3.4
Ts = [10*i for i in range(1,11)]
X_train, X_test, Y_train, Y_test = train_test_split(SX_col, YY, test_size=0.3, shuffle=True)
X_train = X_train.sort_values(by=['TimeStamp'])
X_test = X_test.sort_values(by=['TimeStamp'])
Y_train = Y_train.sort_values(by=['TimeStamp'])
Y_test = Y_test.sort_values(by=['TimeStamp'])
XX = X_train
YY = Y_train

amount={T: 0 for T in Ts}
errors={T: [] for T in Ts}
for T in Ts:
    X = XX[~XX.iloc[:,1:].gt(T).any(1)]
    X = X[~X.iloc[:,1:].lt(-T).any(1)]
    Y = YY.loc[X.index]
    amount[T] = XX.shape[0] - X.shape[0]

    rf_reg = RandomForestRegressor(n_jobs=-1, n_estimators=10)
    rf_reg.fit(X.iloc[:,1:], Y['DispFrames'])
    rf_y = rf_reg.predict(X_test.iloc[:,1:])
    errors[T] += [mean_absolute_error(Y_test['DispFrames'], rf_y) / np.mean(Y_test['DispFrames'])]

plt.plot(Ts, [amount[T] for T in Ts], '.b-')
plt.xlabel('T (threshold)')
plt.ylabel('Number of outliers')
plt.savefig('number_outliers.pdf')
plt.clf()

plt.plot(Ts, [errors[T] for T in Ts], '.b-')
plt.xlabel('T (threshold)')
plt.ylabel('Normalized Mean Absolute Error')
plt.savefig('nmae_outliers.pdf')
plt.clf()
