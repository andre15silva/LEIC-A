import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import random
from sklearn.preprocessing import scale
from sklearn.preprocessing import LabelEncoder
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error

def calculate_percentile(percentile, histogram, clf):
    cumulative = [sum(histogram[:i+1]) for i in range(len(histogram))]
    for i, k in enumerate(clf.classes_):
        if (cumulative[i]/sum(histogram)) >= percentile:
            return k

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

# Split dataset
X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.3, shuffle=True)
X_train = X_train.sort_values(by=['TimeStamp'])
X_test = X_test.sort_values(by=['TimeStamp'])
Y_train = Y_train.sort_values(by=['TimeStamp'])
Y_test = Y_test.sort_values(by=['TimeStamp'])

clf = RandomForestClassifier(n_estimators=10, n_jobs=-1)
clf.fit(X_train.iloc[:,1:], Y_train['frames_bin'])
clf_y = clf.predict(X_test.iloc[:,1:])
print(mean_absolute_error(Y_test['frames_bin'], clf_y) / np.mean(Y_test['frames_bin']))

i = random.choice(range(X_test.shape[0]))
print("Measured target value for " + str(i) + ": " + str(Y_test.iloc[i,:]['DispFrames']))
clf_y_proba = clf.predict_proba(np.array(X_test.iloc[i,1:]).reshape(1,-1))

percentile_20 = calculate_percentile(0.20, clf_y_proba[0], clf)
print("20th percentile: " + str(percentile_20))
percentile_50 = calculate_percentile(0.50, clf_y_proba[0], clf)
print("50th percentile: " + str(percentile_50))
percentile_95 = calculate_percentile(0.95, clf_y_proba[0], clf)
print("95th percentile: " + str(percentile_95))

clf_y_proba_fake = []
for j, k in enumerate(clf.classes_):
    clf_y_proba_fake += int(100*(clf_y_proba[0][j]))*[k]
plt.hist(clf_y_proba_fake, bins=[-0.5+i for i in range(32)], density=True)
plt.xlabel('Y: Displayed frames')
plt.ylabel('P(Y|X=x)')
plt.ylim(0.0, 1.0)
plt.vlines(percentile_20, color='r', ymin=0.0, ymax=1.0, label='20th percentile')
plt.vlines(percentile_50, color='y', ymin=0.0, ymax=1.0, label='50th percentile')
plt.vlines(percentile_95, color='g', ymin=0.0, ymax=1.0, label='95th percentile')
plt.legend()
plt.savefig('P(Y|X=x)_' + str(i) + '_percentiles.pdf')
plt.clf()

pltlimit=3600
X_test_3600 = X_test[X_test['TimeStamp']<=pltlimit]
plt.plot(X_test[X_test['TimeStamp']<=pltlimit].iloc[:,0], Y_test[Y_test['TimeStamp']<=pltlimit]['DispFrames'], color='gray', linestyle='-', label="Measured")
plt.plot(X_test_3600.iloc[:,0], [calculate_percentile(0.20, clf.predict_proba(np.array(X_test_3600.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test_3600.shape[0])], 'r-', label="20th percentile")
plt.plot(X_test_3600.iloc[:,0], [calculate_percentile(0.50, clf.predict_proba(np.array(X_test_3600.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test_3600.shape[0])], 'y-', label="50th percentile")
plt.plot(X_test_3600.iloc[:,0], [calculate_percentile(0.95, clf.predict_proba(np.array(X_test_3600.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test_3600.shape[0])], 'g-', label="95th percentile")
plt.axis('tight')
plt.xlabel('Time index')
plt.ylabel('Displayed frames')
plt.legend(loc='upper right', frameon=True)
plt.savefig('percentiles_timeplot.pdf')
plt.clf()

print("Accuracy of 20th percentile: " + \
        str(sum([Y_test.iloc[i,:]['DispFrames'] <= calculate_percentile(0.20, clf.predict_proba(np.array(X_test.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test.shape[0])])/X_test.shape[0]))
print("Accuracy of 50th percentile: " + \
        str(sum([Y_test.iloc[i,:]['DispFrames'] <= calculate_percentile(0.50, clf.predict_proba(np.array(X_test.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test.shape[0])])/X_test.shape[0]))
print("Accuracy of 95th percentile: " + \
        str(sum([Y_test.iloc[i,:]['DispFrames'] <= calculate_percentile(0.95, clf.predict_proba(np.array(X_test.iloc[i,1:]).reshape(1,-1))[0], clf) for i in range(X_test.shape[0])])/X_test.shape[0]))
