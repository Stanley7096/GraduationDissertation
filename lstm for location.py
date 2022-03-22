#!/usr/bin/env python
# coding: utf-8

# In[1]:


import pandas as pd
import numpy as np

from sklearn.metrics import mean_squared_error
from keras.layers import Dense, Flatten, RepeatVector, Dropout, Activation
from keras.models import Sequential
from keras.layers import LSTM
import matplotlib.pyplot as plt
from sklearn.preprocessing import MinMaxScaler

# 读取数据
df = pd.read_csv('LSTM.csv')
col = df.columns[[0, 1, 2, 3]]  # 取0-3列
df = df[col]

# 划分训练集和测试集

values = df.values
# 取第一列即输入的数据（未归一化的）
X = values[:, 0].reshape(-1, 1)
# 数据中的x和y
Y = values[:, 2:4]
# 划分训练集和测试集长度
y_len = len(Y)
test_len = 20
tran_len = y_len - test_len

# 显示需要预测的训练集中的想x和y的值
aa = [x for x in range(tran_len)]
plt.plot(aa, Y[:tran_len, 0].reshape(-1, 1), marker='.', label="x")
plt.plot(aa, Y[:tran_len, 1].reshape(-1, 1), marker='.', label="y")
plt.ylabel('value', size=15)
plt.xlabel('number', size=15)
plt.legend(fontsize=15)
plt.show()

# 显示需要预测的训练集中的想x和y的值
aa = [x for x in range(test_len)]
plt.plot(aa, Y[tran_len:], marker='.', label="actual")
plt.ylabel('value', size=15)
plt.xlabel('number', size=15)
plt.legend(fontsize=15)
plt.show()
#
#
# 对数据进行归一化
scX = MinMaxScaler(feature_range=(0, 1))
scY = MinMaxScaler(feature_range=(0, 1))
# 归一化输入的数据
scaledX = scX.fit_transform(X)
# 归一化输出的数据
scaledY = scY.fit_transform(Y)
# 对数据进行合并（列），然后划分训练集和测试集长度
data = np.concatenate((scaledX, scaledY), axis=1)
data_train = data[:tran_len, :]
data_test = data[tran_len:, :]
# lstm的时间步长
seq_len = 10
# 转换成LSTM所需格式
X_train = np.array([data_train[i: i + seq_len, 0] for i in range(data_train.shape[0] - seq_len)])
y_train = np.array([data_train[i + seq_len, 1:3] for i in range(data_train.shape[0] - seq_len)])
X_test = np.array([data_test[i: i + seq_len, 0] for i in range(data_test.shape[0] - seq_len)])
y_test = np.array([data_test[i + seq_len, 1:3] for i in range(data_test.shape[0] - seq_len)])
X_train = np.reshape(X_train, (X_train.shape[0], X_train.shape[1], 1))
X_test = np.reshape(X_test, (X_test.shape[0], X_test.shape[1], 1))
# 输出各数据的格式（形状）
print(X_train.shape, y_train.shape, X_test.shape, y_test.shape)

# In[10]:


# 构建和训练网络

# 网络模型
model = Sequential()
model.add(LSTM(240, input_shape=(X_train.shape[1], X_train.shape[2]), return_sequences=True))
model.add(Dropout(0.1))
model.add(LSTM(240, return_sequences=True))
model.add(Dropout(0.1))
model.add(LSTM(256, return_sequences=False))
model.add(Dropout(0.1))
model.add(Dense(y_train.shape[1]))
model.add(Activation("tanh"))
model.compile(loss='mse', optimizer='adam', metrics=['acc'])

history = model.fit(X_train, y_train, epochs=5000, batch_size=40, validation_data=(X_test, y_test), verbose=2)

# 显示训练的loss值情况
plt.plot(history.history['loss'])
plt.plot(history.history['val_loss'])
plt.plot(history.history['acc'])
plt.title('model loss')
plt.ylabel('loss')
plt.xlabel('epoch')
plt.legend(['train', 'test'], loc='upper right')
plt.show()

# 对测试集数据做预测
yhat = model.predict(X_test)
# 反归一化预测值
inv_yhat = scY.inverse_transform(yhat)
# 反归一化真实值
inv_y = scY.inverse_transform(y_test)
# 计算 RMSE
rmse = np.sqrt(mean_squared_error(inv_y, inv_yhat))
print('Test RMSE: %.3f' % rmse)

# 显示预测效果（x，y）坐标形式显示，pre为预测的坐标，true为原来真实坐标
for i in range(len(y_test)):
    point_pre = 'point' + str(i) + 'pre'
    point_true = 'point' + str(i) + 'true'
    plt.scatter(inv_yhat[i, 0], inv_yhat[i, 1], c='r', marker='o', label=point_pre)
    plt.scatter(inv_y[i, 0], inv_y[i, 1], c='g', marker='o', label=point_true)
plt.legend(loc='upper left')
plt.xlabel('location x')
plt.ylabel('location y')
plt.grid()
plt.show()
