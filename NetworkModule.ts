import { NativeEventEmitter, NativeModule, NativeModules } from 'react-native'

const NetworkModule: INetworkModule = NativeModules.NetworkModule
const NetworkEmitter = new NativeEventEmitter(NetworkModule)

interface INetworkState {
  isValidated: boolean
  isVPN: boolean
  isInternetCapable: boolean
  isNetworkSuspended: boolean
  isNetworkRestricted: boolean
}

interface INetworkTransport {
  isCellular: boolean
  isBluetooth: boolean
  isWifi: boolean
  isVPN: boolean
  isEthernet: boolean
  isWifiAware: boolean
  isUSB: boolean
  isLoWPAN: boolean
}

interface INetworkEvent {
  capabilities: INetworkState
  transports: INetworkTransport
}

interface INetworkModule extends NativeModule {
  getNetworkState: () => Promise<INetworkState>
  getNetworkTransport: () => Promise<INetworkTransport>
}

const { getNetworkState, getNetworkTransport } = NetworkModule

const addNetworkListener = (listener: (event: INetworkEvent) => void) => {
  return NetworkEmitter.addListener(
    'NetworkModule.NetworkListenerEvent',
    listener,
  )
}

export { getNetworkState, getNetworkTransport, addNetworkListener }
