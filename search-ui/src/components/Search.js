import "./Search.css";
import qs from 'qs'
import React from 'react';
import {Button, Input, Dropdown} from 'antd'
import {useRequest, useSetState, useUpdateEffect} from 'ahooks'

export default function Search({ serverUrl, params = {}, onResultChange, onReset }) {
    const search = data => fetch(serverUrl + '/api/search?' + qs.stringify(data))
        .then(res => res.json())
        .then(res => {return {...res, query: data.query}})

    const [state, setState] = useSetState({query: ''})

    const wordRequest = useRequest(search, {
        manual: true, onSuccess: data => {
            console.log(data);
            if (data.query !== state.query) return
            const texts = data.articles.filter(item => item.txt).map(item => item.txt)
            const words = []
            for (let i = 0; i < texts.length; i++) {
                const text = texts[i]
                const s = text.indexOf(state.query)
                let flag = 0, word = ''
                for (let j = s; j < text.length && flag < 3 && s >= 0; j++) {
                    if (text.charAt(j) === ' ') {
                      flag++
                    }
                    word = text.substring(s, j)
                }
                if (word.length > 20) {
                    word = word.substr(0, 20)
                }
                words.push(word)
            }
            setState({words: words})
        }
    })

    const searchRequest = useRequest(search, {
        manual: true, onSuccess: data => {
            onResultChange(data, state.query)
            setState({words: []})
        }
    })

    useUpdateEffect(() => {searchRequest.run({query: state.query, ...params})}, [params])

    const handlePressSearch = () => {
        if (!state.query)
            return searchRequest.run({query: state.query, ...params, offset: 0})
        onReset()
    }

    return (
        <div style={{display: "inline-block"}}>
            <Dropdown
                overlay={
                    <div className={'dropdown'}>
                        {state.words && state.words.map((item, index) => (
                            <div className={'word'}
                                 key={index}
                                 onClick={() => {
                                   setState({query: item})
                                   searchRequest.run({query: state.query, ...params, offset: 0})
                                   onReset()
                                 }}
                            >
                              {item}
                            </div>
                        ))}
                    </div>
                }
                visible={state.words && state.words.length > 0}>
                <Input style={{width: 440}}
                       className={'input'} value={state.query}
                       placeholder={'search'}
                       onBlur={() => setTimeout(() => setState({words: []}), 100)}
                       onChange={event => {
                           const v = event.target.value
                           setState({query: event.target.value})
                           if (!v) {
                               setState({words: []})
                               return
                           }
                           wordRequest.run({query: v})
                       }}
                       onPressEnter={handlePressSearch}
                />
            </Dropdown>
            <Button style={{marginLeft: 16}}
                    loading={searchRequest.loading}
                    type={"primary"}
                    onClick={handlePressSearch}
            >
              Search
            </Button>
        </div>
    )
}
