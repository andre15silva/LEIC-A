import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import random
from sklearn.preprocessing import scale
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error

XX = pd.read_csv("VoD flashcrowd - JNSM 2017/X.csv",
        converters={'TimeStamp': pd.Timestamp})
YY = pd.read_csv("VoD flashcrowd - JNSM 2017/Y.csv",
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
X = XX[~XX.iloc[:,1:].gt(T).any(1)]
X = X[~X.iloc[:,1:].lt(-T).any(1)]
Y = YY.loc[X.index]

# Convert to bins
Y['frames_bin'] = pd.cut(Y['DispFrames'], 
        bins=[-0.5+i for i in range(32)],
        labels=[i for i in range(31)],
        include_lowest=True)
Y['frames_bin'] = Y.frames_bin.astype(int)

plt.hist(Y['frames_bin'], bins=[-0.5+i for i in range(1,32)])
plt.xlabel('Displayed frames')
plt.ylabel('Absolute frequency')
plt.savefig('discretized_frames_hist.pdf')
plt.clf()

# Split dataset
X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.3, shuffle=True)

clf = RandomForestClassifier(n_estimators=10, n_jobs=-1)
clf.fit(X_train, Y_train['frames_bin'])

clf_y = clf.predict(X_test)
print(mean_absolute_error(Y_test['frames_bin'], clf_y) / np.mean(Y_test['frames_bin']))

for i in [random.choice(range(X_test.shape[0])) for i in range(2)]:
    print("Measured target value for " + str(i) + ": " + str(Y_test.iloc[i,:]['DispFrames']))
    clf_y_proba = clf.predict_proba(np.array(X_test.iloc[i,:]).reshape(1,-1))
    clf_y_proba_fake = []
    for j, k in enumerate(clf.classes_):
        clf_y_proba_fake += int(100*(clf_y_proba[0][j]))*[k]

    plt.hist(clf_y_proba_fake, bins=[-0.5+i for i in range(32)], density=True)
    plt.xlabel('Y: Displayed frames')
    plt.ylabel('P(Y|X=x)')
    plt.ylim(0.0, 1.0)
    plt.savefig('P(Y|X=x)_' + str(i) + '.pdf')
    plt.clf()
