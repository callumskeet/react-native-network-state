/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * Generated with the TypeScript template
 * https://github.com/react-native-community/react-native-template-typescript
 *
 * @format
 */

import React, { useEffect, useState } from 'react'
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native'

import { Colors } from 'react-native/Libraries/NewAppScreen'
import {
  getNetworkState,
  getNetworkTransport,
  addNetworkListener,
} from './NetworkModule'

const Section: React.FC<{
  title: string
}> = ({ children, title }) => {
  const isDarkMode = useColorScheme() === 'dark'
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  )
}

const App = () => {
  const isDarkMode = useColorScheme() === 'dark'

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  }

  const [networkState, setNetworkState] = useState<any>()
  const [networkInfo, setNetworkInfo] = useState<any>()

  // useEffect(() => {
  //   const updateNetworkState = async () => {
  //     try {
  //       const newNetworkState = await getNetworkState()
  //       const newNetworkInfo = await getNetworkTransport()
  //       setNetworkState(newNetworkState)
  //       setNetworkInfo(newNetworkInfo)
  //     } catch (error) {
  //       console.warn(error)
  //     }
  //   }

  //   const interval = setInterval(() => updateNetworkState(), 2000)
  //   return () => clearInterval(interval)
  // }, [])

  useEffect(() => {
    const subscription = addNetworkListener(event => {
      console.log(event)
      setNetworkState(event)
    })
    return () => subscription.remove()
  }, [])

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Section title="Instantaneous network check">
            <Text>{JSON.stringify(networkState, null, 2)}</Text>
            <Text>{JSON.stringify(networkInfo, null, 2)}</Text>
          </Section>
        </View>
      </ScrollView>
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
})

export default App
