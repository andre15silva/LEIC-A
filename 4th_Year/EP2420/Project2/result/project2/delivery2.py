import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from keras.models import Sequential
from keras.layers import LSTM
from keras.layers import Dense
from sklearn.preprocessing import scale
from sklearn.ensemble import ExtraTreesRegressor
from sklearn.feature_selection import SelectFromModel
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error

XX = pd.read_csv("./KV periodic - JNSM 2017/X.csv",
        converters={'TimeStamp': pd.Timestamp})
YY = pd.read_csv("./KV periodic - JNSM 2017/Y.csv",
        converters={'TimeStamp': pd.Timestamp})

#XX = XX.iloc[:1000,:]
#YY = YY.iloc[:1000,:]

# Convert timestamps to timedeltas
XX.iloc[:,0] = (XX.iloc[:,0] - XX.iloc[0,0]) // pd.Timedelta('1s')
YY.iloc[:,0] = (YY.iloc[:,0] - YY.iloc[0,0]) // pd.Timedelta('1s')

# Pre-process
XX.iloc[:,1:] = scale(XX.iloc[:,1:], axis=0)

# Remove outliers
T = 40
XX = XX[~XX.iloc[:,1:].gt(T).any(1)]
XX = XX[~XX.iloc[:,1:].lt(-T).any(1)]
YY = YY.loc[XX.index]

# Feature selection
clf = ExtraTreesRegressor(n_estimators=10)
clf = clf.fit(XX.iloc[:,1:], YY['ReadsAvg'])
model = SelectFromModel(clf, prefit=True, max_features=16)

X = model.transform(XX.iloc[:,1:])
y = YY['ReadsAvg']

# Split training and testing
T = int(0.7 * X.shape[0])
XX_train = X[:T,:]
yy_train = np.array(y[:T])
XX_test = X[T:,:]
yy_test = np.array(y[T:])

"""
# Transform into sequences
h = 10
nmaes = [[0 for i in range(0,11)] for j in range(0,11)]
for l in range(0,11):
    X_train = np.ndarray((len(range(l, XX_train.shape[0] - h)), l+1, 16))
    for i, t in enumerate(range(l, XX_train.shape[0] - h)):
        X_train[i,:,:] = [XX_train[j, :] for j in range(t-l, t+1)]
    Y_train = np.ndarray((len(range(l, yy_train.shape[0] - h)), h+1))
    for i, t in enumerate(range(l, yy_train.shape[0] - h)):
        Y_train[i,:] = [yy_train[j] for j in range(t, t+h+1)]

    X_test = np.ndarray((len(range(l, XX_test.shape[0] - h)), l+1, 16))
    for i, t in enumerate(range(l, XX_test.shape[0] - h)):
        X_test[i,:,:] = [XX_test[j, :] for j in range(t-l, t+1)]
    Y_test = np.ndarray((len(range(l, yy_test.shape[0] - h)), h+1))
    for i, t in enumerate(range(l, yy_test.shape[0] - h)):
        Y_test[i,:] = [yy_test[j] for j in range(t, t+h+1)]

    # Generate model for l=l_i
    model = Sequential()
    model.add(LSTM(units=100, return_sequences=True, input_shape=(X_train.shape[1], X_train.shape[2])))
    model.add(LSTM(units=50, return_sequences=False, input_shape=(X_train.shape[1], X_train.shape[2])))
    model.add(Dense(units=h+1))
    model.compile(loss='mse', optimizer='adam')
    model.fit(X_train , Y_train, epochs=1000)

    lstm_y = model.predict(X_test)

    # Calculate NMAEs
    for j in range(h+1):
        nmaes[j][l] = mean_absolute_error(Y_test[:,j], lstm_y[:,j]) / np.mean(Y_test[:,j])

nmaes = pd.DataFrame(nmaes)

fig, ax = plt.subplots(figsize=(10,9))
sns.heatmap(nmaes, annot=True, fmt=".4f", ax=ax)
ax.set_xlabel("l")
ax.set_ylabel("h")
plt.savefig("lstm_nmae_heatmap.png")
plt.clf()
"""
# Transform into sequences
h = 64
nmaes = [[0 for i in range(0,8)] for j in range(0,8)]
for ii, l in enumerate([0] + [2**i for i in range(0,7)]):
    X_train = np.ndarray((len(range(l, XX_train.shape[0] - h)), l+1, 16))
    for i, t in enumerate(range(l, XX_train.shape[0] - h)):
        X_train[i,:,:] = [XX_train[j, :] for j in range(t-l, t+1)]
    Y_train = np.ndarray((len(range(l, yy_train.shape[0] - h)), h+1))
    for i, t in enumerate(range(l, yy_train.shape[0] - h)):
        Y_train[i,:] = [yy_train[j] for j in range(t, t+h+1)]

    X_test = np.ndarray((len(range(l, XX_test.shape[0] - h)), l+1, 16))
    for i, t in enumerate(range(l, XX_test.shape[0] - h)):
        X_test[i,:,:] = [XX_test[j, :] for j in range(t-l, t+1)]
    Y_test = np.ndarray((len(range(l, yy_test.shape[0] - h)), h+1))
    for i, t in enumerate(range(l, yy_test.shape[0] - h)):
        Y_test[i,:] = [yy_test[j] for j in range(t, t+h+1)]

    # Generate model for l=l_i
    model = Sequential()
    #model.add(LSTM(units=100, return_sequences=True, input_shape=(X_train.shape[1], X_train.shape[2])))
    model.add(LSTM(units=50, return_sequences=False, input_shape=(X_train.shape[1], X_train.shape[2])))
    model.add(Dense(units=h+1))
    model.compile(loss='mse', optimizer='adam')
    model.fit(X_train , Y_train, epochs=100)

    lstm_y = model.predict(X_test)

    # Calculate NMAEs
    for j, k in enumerate([0] + [2**i for i in range(0,7)]):
        nmaes[j][ii] = mean_absolute_error(Y_test[:,k], lstm_y[:,k]) / np.mean(Y_test[:,k])

nmaes = pd.DataFrame(nmaes, columns=[0] + [2**i for i in range(0,7)], index=[0] + [2**i for i in range(0,7)])

fig, ax = plt.subplots(figsize=(10,9))
sns.heatmap(nmaes, annot=True, fmt=".4f", ax=ax)
ax.set_xlabel("l")
ax.set_ylabel("h")
plt.savefig("lstm_exp_nmae_heatmap.png")
plt.clf()
